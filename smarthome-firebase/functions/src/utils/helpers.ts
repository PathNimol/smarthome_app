/**
 * utils/helpers.ts
 * ─────────────────────────────────────────────────────────────
 * Pure helper functions shared across services.
 */

import * as crypto from "crypto";

// ── Timestamps ─────────────────────────────────────────────────

/** Returns the current Unix timestamp in milliseconds. */
export const nowMs = (): number => Date.now();

/** Returns the current Unix timestamp in seconds. */
export const nowSec = (): number => Math.floor(Date.now() / 1000);

// ── IDs ────────────────────────────────────────────────────────

/** Generates a random hex string of `bytes` length (default 16). */
export const generateId = (bytes = 16): string =>
  crypto.randomBytes(bytes).toString("hex");

/** Generates a short, URL-safe ID (8 chars). */
export const shortId = (): string =>
  crypto.randomBytes(4).toString("hex");

// ── HTTP Responses ─────────────────────────────────────────────

export interface ApiResponse<T = unknown> {
  success: boolean;
  data?:   T;
  error?:  string;
  message?: string;
}

export const successResponse = <T>(data: T, message?: string): ApiResponse<T> => ({
  success: true,
  data,
  ...(message && { message }),
});

export const errorResponse = (error: string): ApiResponse => ({
  success: false,
  error,
});

// ── Validation ─────────────────────────────────────────────────

export const isValidEmail = (email: string): boolean =>
  /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

export const isValidDeviceId = (id: string): boolean =>
  /^[a-zA-Z0-9_-]{6,40}$/.test(id);

// ── Sensor Utilities ────────────────────────────────────────────

/**
 * Clamp a number to [min, max].
 */
export const clamp = (value: number, min: number, max: number): number =>
  Math.min(Math.max(value, min), max);

/**
 * Map a raw ADC reading (0-4095 for ESP32 12-bit ADC) to a percentage.
 */
export const adcToPercent = (raw: number, maxRaw = 4095): number =>
  clamp(Math.round((raw / maxRaw) * 100), 0, 100);

/**
 * Determine water level status label from a percentage value.
 */
export const waterLevelStatus = (pct: number): "normal" | "warning" | "critical" => {
  if (pct >= 90) return "critical";
  if (pct >= 70) return "warning";
  return "normal";
};
