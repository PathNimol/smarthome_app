/**
 * index.ts
 * ─────────────────────────────────────────────────────────────
 * Firebase Cloud Functions entry point.
 *
 * Exports:
 *   api              — HTTPS callable Express app (all REST endpoints)
 *   onWaterLevelWrite — DB trigger: water level → alert
 *   onRainStatusWrite — DB trigger: rain detection → alert
 *   onTemperatureWrite— DB trigger: high temp → fan auto-on
 *   onDeviceStatusChange — DB trigger: device online/offline
 *   onRelayStateChange   — DB trigger: relay toggled notification
 *   checkDevicesOnline   — Scheduled: every 5 min
 *   pruneOldSensorHistory— Scheduled: daily cleanup
 */

// Load env vars before anything else
import * as dotenv from "dotenv";
dotenv.config();

// Initialise Firebase Admin (singleton)
import "./config/firebase";

import * as functions from "firebase-functions";
import app from "./app";

// ── HTTPS API ────────────────────────────────────────────────────
export const api = functions
  .runWith({
    memory:        "512MB",
    timeoutSeconds: 60,
    minInstances:   0,   // set to 1 in production to eliminate cold starts
  })
  .https.onRequest(app);

// ── Realtime DB Triggers ─────────────────────────────────────────
export {
  onWaterLevelWrite,
  onRainStatusWrite,
  onTemperatureWrite,
} from "./triggers/sensor.triggers";

export {
  onDeviceStatusChange,
  onRelayStateChange,
} from "./triggers/device.triggers";

// ── Scheduled Functions ──────────────────────────────────────────
export {
  checkDevicesOnline,
  pruneOldSensorHistory,
} from "./triggers/scheduled.triggers";
