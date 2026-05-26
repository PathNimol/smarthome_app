/**
 * services/device.service.ts
 * ─────────────────────────────────────────────────────────────
 * Device registration, heartbeat tracking, and relay control.
 */

import { db } from "../config/firebase";
import {
  DB_PATHS,
  DEVICE_OFFLINE_THRESHOLD_MS,
  RELAY_IDS,
} from "../config/constants";
import { nowMs, generateId, isValidDeviceId } from "../utils/helpers";
import { logger } from "../utils/logger";
import { issueDeviceToken } from "./auth.service";

// ── Types ─────────────────────────────────────────────────────

export interface Device {
  id:        string;
  name:      string;
  type:      string;
  ownerId:   string;
  location:  string;
  status:    "online" | "offline";
  lastPing:  number;
  createdAt: number;
}

export interface RelayState {
  state:     boolean;
  label:     string;
  updatedAt: number;
  updatedBy: string;
  autoMode:  boolean;
}

// ── Device Registration ─────────────────────────────────────────

/**
 * Registers a new ESP32 device and returns its auth token.
 * Only admins/owners should call this endpoint.
 */
export async function registerDevice(
  params: { name: string; type: string; ownerId: string; location: string }
): Promise<{ device: Device; token: string }> {
  const id = `device_${generateId(8)}`;

  if (!isValidDeviceId(id)) throw new Error("Generated invalid device ID");

  const device: Device = {
    id,
    name:      params.name,
    type:      params.type,
    ownerId:   params.ownerId,
    location:  params.location,
    status:    "offline",
    lastPing:  0,
    createdAt: nowMs(),
  };

  const token = issueDeviceToken(id);

  // Write device record and its token (token stored separately for ACL)
  await db.ref(`${DB_PATHS.DEVICES}/${id}`).set(device);
  await db.ref(`${DB_PATHS.DEVICES}/${id}/authToken`).set(token);

  // Initialise default relay states (fan + light)
  await initialiseRelays(id, params.ownerId);

  logger.info("Device registered", { id, name: params.name });
  return { device, token };
}

/** Sets up default relay entries for a newly registered device. */
async function initialiseRelays(deviceId: string, ownerId: string): Promise<void> {
  const defaults: Record<string, RelayState> = {
    [RELAY_IDS.FAN]: {
      state:     false,
      label:     "Fan",
      updatedAt: nowMs(),
      updatedBy: ownerId,
      autoMode:  false,
    },
    [RELAY_IDS.LIGHT]: {
      state:     false,
      label:     "Light",
      updatedAt: nowMs(),
      updatedBy: ownerId,
      autoMode:  false,
    },
    [RELAY_IDS.PUMP]: {
      state:     false,
      label:     "Water Pump",
      updatedAt: nowMs(),
      updatedBy: ownerId,
      autoMode:  false,
    },
  };

  await db.ref(`${DB_PATHS.RELAY_STATES}/${deviceId}`).set(defaults);
}

// ── Heartbeat / Ping ─────────────────────────────────────────────

/**
 * Called by ESP32 on every sensor upload.
 * Updates status → online and lastPing timestamp.
 */
export async function recordHeartbeat(deviceId: string): Promise<void> {
  await db.ref(`${DB_PATHS.DEVICES}/${deviceId}`).update({
    status:   "online",
    lastPing: nowMs(),
  });
}

/**
 * Marks a device as offline if its lastPing is too old.
 * Called by the scheduled Cloud Function.
 */
export async function checkAndMarkOffline(deviceId: string): Promise<boolean> {
  const snap = await db.ref(`${DB_PATHS.DEVICES}/${deviceId}/lastPing`).once("value");
  const lastPing = snap.val() as number | null;

  if (!lastPing || nowMs() - lastPing > DEVICE_OFFLINE_THRESHOLD_MS) {
    await db.ref(`${DB_PATHS.DEVICES}/${deviceId}/status`).set("offline");
    return true;
  }
  return false;
}

/**
 * Returns all devices with their current status.
 */
export async function getAllDevices(): Promise<Device[]> {
  const snap = await db.ref(DB_PATHS.DEVICES).once("value");
  const val  = snap.val() as Record<string, Device> | null;
  return val ? Object.values(val) : [];
}

// ── Relay Control ─────────────────────────────────────────────

/**
 * Toggles or sets a relay state (fan / light / pump).
 * The ESP32 listens to relay_states/<deviceId>/<relayId>/state
 * via the Firebase Realtime Database SDK and reacts instantly.
 */
export async function setRelayState(
  deviceId: string,
  relayId:  string,
  state:    boolean,
  updatedBy: string
): Promise<RelayState> {
  const path = `${DB_PATHS.RELAY_STATES}/${deviceId}/${relayId}`;

  const update: Partial<RelayState> = {
    state,
    updatedAt: nowMs(),
    updatedBy,
  };

  await db.ref(path).update(update);
  const snap = await db.ref(path).once("value");

  logger.info("Relay updated", { deviceId, relayId, state, updatedBy });
  return snap.val() as RelayState;
}

export async function getRelayStates(
  deviceId: string
): Promise<Record<string, RelayState>> {
  const snap = await db
    .ref(`${DB_PATHS.RELAY_STATES}/${deviceId}`)
    .once("value");
  return (snap.val() as Record<string, RelayState>) || {};
}
