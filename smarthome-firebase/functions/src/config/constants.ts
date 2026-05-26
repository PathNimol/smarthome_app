/**
 * config/constants.ts
 * ─────────────────────────────────────────────────────────────
 * Central place for every magic number, threshold, and
 * path constant used throughout the backend.
 */

// ── Sensor Thresholds ──────────────────────────────────────────
export const THRESHOLDS = {
  WATER_LEVEL: {
    WARNING:  Number(process.env.WATER_LEVEL_WARNING_THRESHOLD)  || 70,  // %
    CRITICAL: Number(process.env.WATER_LEVEL_CRITICAL_THRESHOLD) || 90,  // %
  },
  RAIN: {
    INTENSITY: Number(process.env.RAIN_INTENSITY_THRESHOLD) || 30,       // 0-100
  },
  TEMPERATURE: {
    HIGH: 40,   // °C
    LOW:  0,    // °C
  },
  HUMIDITY: {
    HIGH: 95,   // %
  },
} as const;

// ── Device Offline Detection ────────────────────────────────────
export const DEVICE_OFFLINE_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes

// ── Realtime Database Paths ─────────────────────────────────────
export const DB_PATHS = {
  USERS:          "users",
  DEVICES:        "devices",
  SENSORS:        "sensors",
  RELAY_STATES:   "relay_states",
  NOTIFICATIONS:  "notifications",
  ACTIVITY_LOGS:  "activity_logs",
  SENSOR_HISTORY: "sensor_history",
} as const;

// ── Notification Types ──────────────────────────────────────────
export const NOTIFICATION_TYPES = {
  WATER_LEVEL_WARNING:  "water_level_warning",
  WATER_LEVEL_CRITICAL: "water_level_critical",
  RAIN_DETECTED:        "rain_detected",
  DEVICE_OFFLINE:       "device_offline",
  DEVICE_ONLINE:        "device_online",
  RELAY_CHANGED:        "relay_changed",
  TEMPERATURE_HIGH:     "temperature_high",
} as const;

// ── User Roles ──────────────────────────────────────────────────
export const ROLES = {
  ADMIN:  "admin",
  USER:   "user",
  VIEWER: "viewer",
} as const;

export type UserRole = typeof ROLES[keyof typeof ROLES];

// ── Relay IDs ───────────────────────────────────────────────────
export const RELAY_IDS = {
  FAN:   "relay_1",
  LIGHT: "relay_2",
  PUMP:  "relay_3",
} as const;
