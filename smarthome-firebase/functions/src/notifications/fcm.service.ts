/**
 * notifications/fcm.service.ts
 * ─────────────────────────────────────────────────────────────
 * All Firebase Cloud Messaging (FCM) dispatch logic.
 *
 * Supports:
 *  - Single-token unicast
 *  - Multi-token multicast (sendEachForMulticast)
 *  - Topic-based broadcast
 *  - Notification storage in DB (per-user inbox)
 */

import { messaging, db } from "../config/firebase";
import { Message, MulticastMessage } from "firebase-admin/messaging";
import { DB_PATHS, NOTIFICATION_TYPES } from "../config/constants";
import { getAllUserTokens, getFcmTokens } from "../services/auth.service";
import { nowMs, generateId } from "../utils/helpers";
import { logger } from "../utils/logger";

// ── Types ────────────────────────────────────────────────────

export interface NotificationPayload {
  type:     string;
  title:    string;
  body:     string;
  deviceId: string;
  data?:    Record<string, string>;
}

export interface DbNotification extends NotificationPayload {
  id:        string;
  timestamp: number;
  read:      boolean;
}

// ── Core send helpers ─────────────────────────────────────────

/**
 * Sends a push notification to a single FCM token.
 */
export async function sendToToken(
  token:   string,
  payload: NotificationPayload
): Promise<void> {
  const message: Message = {
    token,
    notification: { title: payload.title, body: payload.body },
    data: {
      type:     payload.type,
      deviceId: payload.deviceId,
      ...payload.data,
    },
    android: {
      priority: "high",
      notification: {
        sound:       "default",
        channelId:   channelForType(payload.type),
        priority:    "high",
        defaultSound: true,
      },
    },
    apns: {
      payload: {
        aps: {
          sound: "default",
          badge: 1,
          contentAvailable: true,
        },
      },
    },
  };

  try {
    await messaging.send(message);
  } catch (err: unknown) {
    // Token might be stale — log and continue
    logger.warn("FCM send failed for token", { token: token.slice(-8), error: String(err) });
  }
}

/**
 * Sends a push notification to multiple tokens at once (multicast).
 * Automatically cleans up invalid tokens.
 */
export async function sendToTokens(
  tokens:  string[],
  payload: NotificationPayload
): Promise<void> {
  if (tokens.length === 0) return;

  const message: MulticastMessage = {
    tokens,
    notification: { title: payload.title, body: payload.body },
    data: {
      type:     payload.type,
      deviceId: payload.deviceId,
      timestamp: String(nowMs()),
      ...payload.data,
    },
    android: {
      priority: "high",
      notification: {
        sound:     "default",
        channelId: channelForType(payload.type),
      },
    },
    apns: {
      payload: { aps: { sound: "default", badge: 1 } },
    },
  };

  const batchResponse = await messaging.sendEachForMulticast(message);

  logger.info("FCM multicast sent", {
    successCount: batchResponse.successCount,
    failureCount: batchResponse.failureCount,
  });

  // Log failures per token
  batchResponse.responses.forEach((resp, idx) => {
    if (!resp.success) {
      logger.warn("FCM token error", {
        token: tokens[idx].slice(-8),
        error: resp.error?.message,
      });
    }
  });
}

/**
 * Broadcasts a notification to ALL registered users.
 * Saves the notification to each user's DB inbox.
 */
export async function broadcastToAllUsers(
  payload: NotificationPayload
): Promise<void> {
  const allUsers = await getAllUserTokens();

  const tokenSendPromises = allUsers.map(({ uid, tokens }) =>
    Promise.all([
      sendToTokens(tokens, payload),
      saveNotificationToDb(uid, payload),
    ])
  );

  await Promise.all(tokenSendPromises);
  logger.info("Broadcast sent", { type: payload.type, recipients: allUsers.length });
}

/**
 * Sends a notification to a specific user (all their devices).
 */
export async function sendToUser(
  uid:     string,
  payload: NotificationPayload
): Promise<void> {
  const tokens = await getFcmTokens(uid);
  await Promise.all([
    sendToTokens(tokens, payload),
    saveNotificationToDb(uid, payload),
  ]);
}

// ── Pre-built notification factories ─────────────────────────

export async function notifyWaterLevelWarning(
  deviceId: string,
  level:    number
): Promise<void> {
  await broadcastToAllUsers({
    type:     NOTIFICATION_TYPES.WATER_LEVEL_WARNING,
    title:    "⚠️ Water Level Warning",
    body:     `Water level has reached ${level}%. Check the tank soon.`,
    deviceId,
    data:     { level: String(level) },
  });
}

export async function notifyWaterLevelCritical(
  deviceId: string,
  level:    number
): Promise<void> {
  await broadcastToAllUsers({
    type:     NOTIFICATION_TYPES.WATER_LEVEL_CRITICAL,
    title:    "🚨 Critical Water Level!",
    body:     `Water level is at ${level}%! Immediate action required.`,
    deviceId,
    data:     { level: String(level) },
  });
}

export async function notifyRainDetected(
  deviceId:  string,
  intensity: number
): Promise<void> {
  await broadcastToAllUsers({
    type:     NOTIFICATION_TYPES.RAIN_DETECTED,
    title:    "🌧️ Rain Detected",
    body:     `Rain sensor reports ${intensity}% intensity. Auto-actions may trigger.`,
    deviceId,
    data:     { intensity: String(intensity) },
  });
}

export async function notifyDeviceOffline(deviceId: string): Promise<void> {
  await broadcastToAllUsers({
    type:     NOTIFICATION_TYPES.DEVICE_OFFLINE,
    title:    "Device Offline",
    body:     `Device ${deviceId} has not responded for 5 minutes.`,
    deviceId,
  });
}

export async function notifyDeviceOnline(deviceId: string): Promise<void> {
  await broadcastToAllUsers({
    type:     NOTIFICATION_TYPES.DEVICE_ONLINE,
    title:    "Device Back Online",
    body:     `Device ${deviceId} is online again.`,
    deviceId,
  });
}

export async function notifyRelayChanged(
  deviceId:  string,
  relayLabel: string,
  state:     boolean,
  changedBy: string
): Promise<void> {
  await broadcastToAllUsers({
    type:     NOTIFICATION_TYPES.RELAY_CHANGED,
    title:    `${state ? "🟢" : "🔴"} ${relayLabel} ${state ? "ON" : "OFF"}`,
    body:     `${relayLabel} was turned ${state ? "on" : "off"} by ${changedBy}.`,
    deviceId,
    data:     { relayLabel, state: String(state), changedBy },
  });
}

// ── DB Notification Inbox ─────────────────────────────────────

/**
 * Saves a notification to the user's DB inbox for in-app display.
 */
async function saveNotificationToDb(
  uid:     string,
  payload: NotificationPayload
): Promise<void> {
  const notif: DbNotification = {
    id:        generateId(8),
    ...payload,
    timestamp: nowMs(),
    read:      false,
  };
  await db
    .ref(`${DB_PATHS.NOTIFICATIONS}/${uid}/${notif.id}`)
    .set(notif);
}

/**
 * Marks a notification as read.
 */
export async function markNotificationRead(
  uid:    string,
  notifId: string
): Promise<void> {
  await db
    .ref(`${DB_PATHS.NOTIFICATIONS}/${uid}/${notifId}/read`)
    .set(true);
}

// ── Helpers ───────────────────────────────────────────────────

function channelForType(type: string): string {
  const criticalTypes = [
    NOTIFICATION_TYPES.WATER_LEVEL_CRITICAL,
    NOTIFICATION_TYPES.RAIN_DETECTED,
  ];
  return criticalTypes.includes(type) ? "alerts_critical" : "alerts_general";
}
