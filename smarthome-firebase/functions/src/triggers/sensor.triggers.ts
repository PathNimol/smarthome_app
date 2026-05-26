/**
 * triggers/sensor.triggers.ts
 * ─────────────────────────────────────────────────────────────
 * Firebase Realtime Database triggers that fire whenever sensor
 * data is written.  These Cloud Functions:
 *
 *  1. Check thresholds
 *  2. Emit FCM push notifications
 *  3. Optionally auto-control relays
 *  4. Write activity logs
 *
 * Note: These run server-side and are NOT callable by clients.
 */

import * as functions from "firebase-functions";
import { THRESHOLDS, NOTIFICATION_TYPES, RELAY_IDS } from "../config/constants";
import {
  notifyWaterLevelWarning,
  notifyWaterLevelCritical,
  notifyRainDetected,
} from "../notifications/fcm.service";
import { setRelayState } from "../services/device.service";
import { logActivity }   from "../services/activity.service";
import { logger }        from "../utils/logger";

// ── Water Level Trigger ─────────────────────────────────────────

/**
 * Fires when any device writes a new water_level reading.
 * Path: sensors/{deviceId}/water_level
 */
export const onWaterLevelWrite = functions.database
  .ref("sensors/{deviceId}/water_level")
  .onWrite(async (change, context) => {
    const deviceId = context.params.deviceId as string;
    const newVal   = change.after.val() as {
      value: number;
      status: string;
      timestamp: number;
    } | null;

    if (!newVal) return; // Data deleted — ignore

    const { value: level, status } = newVal;

    logger.info("Water level trigger", { deviceId, level, status });

    // ── Critical threshold (≥ 90%) ──
    if (level >= THRESHOLDS.WATER_LEVEL.CRITICAL) {
      await notifyWaterLevelCritical(deviceId, level);
      await logActivity(
        NOTIFICATION_TYPES.WATER_LEVEL_CRITICAL,
        "system",
        deviceId,
        { level }
      );

      // Auto-start the water pump to drain
      try {
        await setRelayState(deviceId, RELAY_IDS.PUMP, true, "system_auto");
        logger.info("Auto-started pump due to critical water level", { deviceId });
      } catch (err) {
        logger.error("Failed to auto-start pump", { error: String(err) });
      }
      return;
    }

    // ── Warning threshold (≥ 70%) ──
    if (level >= THRESHOLDS.WATER_LEVEL.WARNING) {
      await notifyWaterLevelWarning(deviceId, level);
      await logActivity(
        NOTIFICATION_TYPES.WATER_LEVEL_WARNING,
        "system",
        deviceId,
        { level }
      );
      return;
    }

    // ── Level back to normal — turn off pump if it was auto-started ──
    if (level < THRESHOLDS.WATER_LEVEL.WARNING) {
      try {
        await setRelayState(deviceId, RELAY_IDS.PUMP, false, "system_auto");
      } catch {
        // Pump might not exist; ignore
      }
    }
  });

// ── Rain Detection Trigger ──────────────────────────────────────

/**
 * Fires when any device writes a new rain_status reading.
 * Path: sensors/{deviceId}/rain_status
 */
export const onRainStatusWrite = functions.database
  .ref("sensors/{deviceId}/rain_status")
  .onWrite(async (change, context) => {
    const deviceId = context.params.deviceId as string;
    const newVal   = change.after.val() as {
      isRaining:  boolean;
      intensity:  number;
      timestamp:  number;
    } | null;

    if (!newVal) return;

    const { isRaining, intensity } = newVal;

    // Only alert when it starts raining (not on every tick)
    const prevVal = change.before.val() as { isRaining: boolean } | null;
    const justStarted = isRaining && (!prevVal || !prevVal.isRaining);

    if (justStarted && intensity >= THRESHOLDS.RAIN.INTENSITY) {
      logger.info("Rain started trigger", { deviceId, intensity });

      await notifyRainDetected(deviceId, intensity);
      await logActivity(
        NOTIFICATION_TYPES.RAIN_DETECTED,
        "system",
        deviceId,
        { intensity }
      );
    }
  });

// ── Temperature High Trigger ────────────────────────────────────

/**
 * Path: sensors/{deviceId}/temperature
 */
export const onTemperatureWrite = functions.database
  .ref("sensors/{deviceId}/temperature")
  .onWrite(async (change, context) => {
    const deviceId = context.params.deviceId as string;
    const newVal   = change.after.val() as { value: number } | null;
    if (!newVal) return;

    const { value: temp } = newVal;

    if (temp >= THRESHOLDS.TEMPERATURE.HIGH) {
      logger.warn("High temperature detected", { deviceId, temp });

      // Auto-turn on fan when temperature is high
      try {
        await setRelayState(deviceId, RELAY_IDS.FAN, true, "system_auto");
        await logActivity(
          NOTIFICATION_TYPES.TEMPERATURE_HIGH,
          "system",
          deviceId,
          { temperature: temp }
        );
      } catch (err) {
        logger.error("Failed to auto-start fan", { error: String(err) });
      }
    }
  });
