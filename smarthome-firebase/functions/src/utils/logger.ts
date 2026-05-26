/**
 * utils/logger.ts
 * ─────────────────────────────────────────────────────────────
 * Structured logger built on Winston.
 * In production Cloud Functions, logs are surfaced in
 * Google Cloud Logging with severity levels.
 */

import { createLogger, format, transports } from "winston";

const { combine, timestamp, printf, colorize, errors } = format;

const logFormat = printf(({ level, message, timestamp: ts, stack, ...meta }) => {
  const metaStr = Object.keys(meta).length ? ` ${JSON.stringify(meta)}` : "";
  return `[${ts}] ${level.toUpperCase()}: ${stack || message}${metaStr}`;
});

export const logger = createLogger({
  level: process.env.LOG_LEVEL || "info",
  format: combine(
    errors({ stack: true }),
    timestamp({ format: "YYYY-MM-DD HH:mm:ss" }),
    process.env.NODE_ENV === "production"
      ? format.json()               // structured JSON for Cloud Logging
      : combine(colorize(), logFormat)
  ),
  transports: [new transports.Console()],
  silent: process.env.NODE_ENV === "test",
});
