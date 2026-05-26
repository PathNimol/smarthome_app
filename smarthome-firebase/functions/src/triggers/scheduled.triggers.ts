/**
 * triggers/scheduled.triggers.ts
 * ─────────────────────────────────────────────────────────────
 * Scheduled (cron) Cloud Functions:
 *  - Device offline checker (runs every 5 minutes)
 *  - Sensor history pruner  (runs daily)
 */

import * as functions from "firebase-functions";
import { db }         from "../config/firebase";
import { DB_PATHS }   from "../config/constants";
import { checkAndMarkOffline } from "../services/device.service";
import { notifyDeviceOffline } from "../notifications/fcm.service";
import { logActivity }         from "../services/activity.service";
import { logger }              from "../utils/logger";

// ── Device Offline Checker ──────────────────────────────────────

/**
 * Runs every 5 minutes.
 * Marks devices offline if they haven't sent a heartbeat recently.
 */
export const checkDevicesOnline = functions.pubsub
  .schedule("every 5 minutes")
  .onRun(async () => {
    logger.info("Running device online check");

    const snap = await db.ref(DB_PATHS.DEVICES).once("value");
    const devices = snap.val() as Record<string, { status: string }> | null;
    if (!devices) return;

    const checks = Object.keys(devices).map(async (deviceId) => {
      const wentOffline = await checkAndMarkOffline(deviceId);
      if (wentOffline && devices[deviceId].status === "online") {
        // Only notify on transition online → offline
        await notifyDeviceOffline(deviceId);
        await logActivity("device_offline_auto", "system", deviceId, {});
        logger.warn("Device marked offline", { deviceId });
      }
    });

    await Promise.all(checks);
    logger.info("Device online check complete", { total: Object.keys(devices).length });
  });

// ── Sensor History Pruner ────────────────────────────────────────

/**
 * Runs once a day at 02:00 UTC.
 * Removes sensor history older than 30 days to control DB size.
 */
export const pruneOldSensorHistory = functions.pubsub
  .schedule("0 2 * * *")
  .timeZone("UTC")
  .onRun(async () => {
    const thirtyDaysAgo = Date.now() - 30 * 24 * 60 * 60 * 1000;

    logger.info("Pruning sensor history older than 30 days");

    const devicesSnap = await db.ref(DB_PATHS.DEVICES).once("value");
    const devices = devicesSnap.val() as Record<string, unknown> | null;
    if (!devices) return;

    const sensorTypes = ["water_level", "rain_status", "temperature", "humidity"];

    for (const deviceId of Object.keys(devices)) {
      for (const sensorType of sensorTypes) {
        const histSnap = await db
          .ref(`${DB_PATHS.SENSOR_HISTORY}/${deviceId}/${sensorType}`)
          .orderByChild("timestamp")
          .endAt(thirtyDaysAgo)
          .once("value");

        if (!histSnap.exists()) continue;

        const updates: Record<string, null> = {};
        histSnap.forEach((child) => {
          updates[child.key!] = null;
        });

        await db
          .ref(`${DB_PATHS.SENSOR_HISTORY}/${deviceId}/${sensorType}`)
          .update(updates);

        logger.debug("Pruned history", {
          deviceId,
          sensorType,
          removed: Object.keys(updates).length,
        });
      }
    }

    logger.info("Sensor history pruning complete");
  });
