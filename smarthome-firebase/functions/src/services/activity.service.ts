/**
 * services/activity.service.ts
 * ─────────────────────────────────────────────────────────────
 * Append-only activity / audit log stored in the Realtime DB.
 * Useful for admin dashboards and compliance.
 */

import { db } from "../config/firebase";
import { DB_PATHS } from "../config/constants";
import { nowMs, generateId } from "../utils/helpers";
import { logger } from "../utils/logger";

export interface ActivityLog {
  id:        string;
  action:    string;
  userId:    string;
  deviceId:  string;
  timestamp: number;
  details:   Record<string, unknown>;
}

export async function logActivity(
  action:   string,
  userId:   string,
  deviceId: string,
  details:  Record<string, unknown> = {}
): Promise<void> {
  const log: ActivityLog = {
    id:        generateId(6),
    action,
    userId,
    deviceId,
    timestamp: nowMs(),
    details,
  };

  try {
    await db.ref(`${DB_PATHS.ACTIVITY_LOGS}/${log.id}`).set(log);
  } catch (err) {
    // Never let logging failures break the main flow
    logger.error("Failed to write activity log", { error: String(err) });
  }
}

export async function getRecentLogs(limit = 50): Promise<ActivityLog[]> {
  const snap = await db
    .ref(DB_PATHS.ACTIVITY_LOGS)
    .orderByChild("timestamp")
    .limitToLast(limit)
    .once("value");

  const val = snap.val() as Record<string, ActivityLog> | null;
  return val
    ? Object.values(val).sort((a, b) => b.timestamp - a.timestamp)
    : [];
}
