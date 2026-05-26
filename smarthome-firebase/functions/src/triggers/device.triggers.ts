/**
 * triggers/device.triggers.ts
 * ─────────────────────────────────────────────────────────────
 * Triggers for device status transitions and relay changes.
 */

import * as functions from "firebase-functions";
import {
  notifyDeviceOffline,
  notifyDeviceOnline,
  notifyRelayChanged,
} from "../notifications/fcm.service";
import { logActivity } from "../services/activity.service";
import { logger }      from "../utils/logger";

// ── Device Status Change ────────────────────────────────────────

/**
 * Fires when a device's status field changes.
 * Path: devices/{deviceId}/status
 */
export const onDeviceStatusChange = functions.database
  .ref("devices/{deviceId}/status")
  .onWrite(async (change, context) => {
    const deviceId  = context.params.deviceId as string;
    const prevStatus = change.before.val() as string | null;
    const newStatus  = change.after.val()  as string | null;

    if (prevStatus === newStatus) return; // No change

    logger.info("Device status changed", { deviceId, prevStatus, newStatus });

    if (newStatus === "offline" && prevStatus === "online") {
      await notifyDeviceOffline(deviceId);
      await logActivity("device_offline", "system", deviceId, {});
    }

    if (newStatus === "online" && prevStatus === "offline") {
      await notifyDeviceOnline(deviceId);
      await logActivity("device_online", "system", deviceId, {});
    }
  });

// ── Relay State Change ──────────────────────────────────────────

/**
 * Fires when any relay is toggled.
 * Path: relay_states/{deviceId}/{relayId}
 */
export const onRelayStateChange = functions.database
  .ref("relay_states/{deviceId}/{relayId}")
  .onWrite(async (change, context) => {
    const deviceId = context.params.deviceId as string;
    const relayId  = context.params.relayId  as string;

    const newVal  = change.after.val()  as {
      state:     boolean;
      label:     string;
      updatedBy: string;
    } | null;
    const prevVal = change.before.val() as { state: boolean } | null;

    if (!newVal) return;
    if (prevVal?.state === newVal.state) return; // No actual toggle

    logger.info("Relay toggled", {
      deviceId,
      relayId,
      state:     newVal.state,
      updatedBy: newVal.updatedBy,
    });

    // Don't send a push notification for system-auto changes to avoid spam
    if (newVal.updatedBy !== "system_auto") {
      await notifyRelayChanged(
        deviceId,
        newVal.label || relayId,
        newVal.state,
        newVal.updatedBy
      );
    }

    await logActivity("relay_toggled", newVal.updatedBy, deviceId, {
      relayId,
      state: newVal.state,
    });
  });
