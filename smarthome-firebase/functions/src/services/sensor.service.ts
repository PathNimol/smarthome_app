/**
 * services/sensor.service.ts
 * ─────────────────────────────────────────────────────────────
 * Handles all sensor data uploaded by ESP32 devices:
 *   - Water level
 *   - Rain detection
 *   - Temperature / humidity (DHT22 / BME280)
 *
 * Each write goes to sensors/<deviceId>/<type> (latest value)
 * and a timestamped copy to sensor_history/<deviceId>/<type>/<ts>
 */

import { db } from "../config/firebase";
import { DB_PATHS, THRESHOLDS } from "../config/constants";
import { nowMs, adcToPercent, waterLevelStatus } from "../utils/helpers";
import { logger } from "../utils/logger";
import { recordHeartbeat } from "./device.service";

// ── Types ─────────────────────────────────────────────────────

export interface WaterLevelPayload {
  deviceId:  string;
  rawValue?: number;   // 0-4095 ADC
  value?:    number;   // 0-100 % (if device calculates it)
  unit?:     string;
}

export interface RainPayload {
  deviceId:   string;
  rawValue:   number;   // 0-4095 ADC from rain sensor
  isRaining?: boolean;  // device-computed (optional)
}

export interface TemperatureHumidityPayload {
  deviceId:    string;
  temperature: number;  // °C
  humidity:    number;  // %
}

export interface SensorReading {
  value:     number;
  unit:      string;
  timestamp: number;
  status?:   string;
}

export interface WaterLevelReading extends SensorReading {
  status: "normal" | "warning" | "critical";
}

export interface RainReading {
  isRaining:  boolean;
  intensity:  number;   // 0-100 %
  rawValue:   number;
  timestamp:  number;
}

// ── Water Level ───────────────────────────────────────────────

export async function ingestWaterLevel(
  payload: WaterLevelPayload
): Promise<WaterLevelReading> {
  const { deviceId, rawValue, unit = "%" } = payload;

  // Prefer pre-computed %, fall back to ADC mapping
  const pct = payload.value !== undefined
    ? payload.value
    : adcToPercent(rawValue ?? 0);

  const reading: WaterLevelReading = {
    value:     pct,
    unit,
    timestamp: nowMs(),
    status:    waterLevelStatus(pct),
  };

  const ref = db.ref(`${DB_PATHS.SENSORS}/${deviceId}/water_level`);
  await ref.set(reading);

  // Append to history
  await appendHistory(deviceId, "water_level", { value: pct, unit, timestamp: reading.timestamp });

  // Update device heartbeat
  await recordHeartbeat(deviceId);

  logger.debug("Water level ingested", { deviceId, pct, status: reading.status });
  return reading;
}

// ── Rain Detection ────────────────────────────────────────────

export async function ingestRainStatus(
  payload: RainPayload
): Promise<RainReading> {
  const { deviceId, rawValue } = payload;

  // Invert ADC: lower raw value = more rain on most resistive rain sensors
  const intensity = clampIntensity(100 - adcToPercent(rawValue));
  const isRaining = payload.isRaining !== undefined
    ? payload.isRaining
    : intensity >= THRESHOLDS.RAIN.INTENSITY;

  const reading: RainReading = {
    isRaining,
    intensity,
    rawValue,
    timestamp: nowMs(),
  };

  await db.ref(`${DB_PATHS.SENSORS}/${deviceId}/rain_status`).set(reading);
  await appendHistory(deviceId, "rain_status", {
    value: intensity,
    unit:  "%",
    timestamp: reading.timestamp,
  });
  await recordHeartbeat(deviceId);

  logger.debug("Rain status ingested", { deviceId, isRaining, intensity });
  return reading;
}

// ── Temperature & Humidity ────────────────────────────────────

export async function ingestTemperatureHumidity(
  payload: TemperatureHumidityPayload
): Promise<void> {
  const { deviceId, temperature, humidity } = payload;
  const ts = nowMs();

  const tempReading: SensorReading = {
    value:     temperature,
    unit:      "°C",
    timestamp: ts,
  };
  const humReading: SensorReading = {
    value:     humidity,
    unit:      "%",
    timestamp: ts,
  };

  await db.ref(`${DB_PATHS.SENSORS}/${deviceId}/temperature`).set(tempReading);
  await db.ref(`${DB_PATHS.SENSORS}/${deviceId}/humidity`).set(humReading);

  await appendHistory(deviceId, "temperature", { value: temperature, unit: "°C", timestamp: ts });
  await appendHistory(deviceId, "humidity",    { value: humidity,    unit: "%",  timestamp: ts });

  await recordHeartbeat(deviceId);
}

// ── Full Sensor Batch Upload (single ESP32 POST) ──────────────

export interface SensorBatchPayload {
  deviceId:     string;
  waterLevel?:  Omit<WaterLevelPayload, "deviceId">;
  rain?:        Omit<RainPayload, "deviceId">;
  tempHumidity?: Omit<TemperatureHumidityPayload, "deviceId">;
}

export async function ingestSensorBatch(
  payload: SensorBatchPayload
): Promise<void> {
  const { deviceId, waterLevel, rain, tempHumidity } = payload;

  const ops: Promise<unknown>[] = [];

  if (waterLevel)   ops.push(ingestWaterLevel({ ...waterLevel, deviceId }));
  if (rain)         ops.push(ingestRainStatus({ ...rain, deviceId }));
  if (tempHumidity) ops.push(ingestTemperatureHumidity({ ...tempHumidity, deviceId }));

  await Promise.all(ops);
  logger.info("Sensor batch ingested", { deviceId, sensors: Object.keys(payload) });
}

// ── History ───────────────────────────────────────────────────

/**
 * Appends a single reading to the sensor_history time-series.
 * History entries are keyed by timestamp to allow range queries.
 */
async function appendHistory(
  deviceId:   string,
  sensorType: string,
  data:       { value: number; unit: string; timestamp: number }
): Promise<void> {
  const path = `${DB_PATHS.SENSOR_HISTORY}/${deviceId}/${sensorType}/${data.timestamp}`;
  await db.ref(path).set(data);
}

/**
 * Returns the last N sensor history entries for a given type.
 */
export async function getSensorHistory(
  deviceId:   string,
  sensorType: string,
  limit = 100
): Promise<Array<{ value: number; unit: string; timestamp: number }>> {
  const snap = await db
    .ref(`${DB_PATHS.SENSOR_HISTORY}/${deviceId}/${sensorType}`)
    .orderByChild("timestamp")
    .limitToLast(limit)
    .once("value");

  const val = snap.val() as Record<string, { value: number; unit: string; timestamp: number }> | null;
  return val ? Object.values(val).sort((a, b) => a.timestamp - b.timestamp) : [];
}

// ── Helpers ───────────────────────────────────────────────────

function clampIntensity(n: number): number {
  return Math.min(100, Math.max(0, n));
}
