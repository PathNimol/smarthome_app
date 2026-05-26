/**
 * app.ts
 * ─────────────────────────────────────────────────────────────
 * Configures and exports the Express application.
 * The app is mounted as a single Firebase Cloud Function in index.ts.
 */

import express from "express";
import cors    from "cors";
import helmet  from "helmet";

import authRoutes         from "./api/auth.routes";
import deviceRoutes       from "./api/device.routes";
import sensorRoutes       from "./api/sensor.routes";
import notificationRoutes from "./api/notification.routes";

import { errorHandler }   from "./middleware/error.middleware";
import { logger }         from "./utils/logger";

const app = express();

// ── Security headers ────────────────────────────────────────────
app.use(helmet());

// ── CORS ────────────────────────────────────────────────────────
app.use(cors({
  origin: process.env.NODE_ENV === "production"
    ? ["https://yourapp.page.link", /\.firebaseapp\.com$/]
    : true,
  methods:  ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  allowedHeaders: ["Content-Type", "Authorization", "X-Device-Token"],
}));

// ── Body parsing ─────────────────────────────────────────────────
app.use(express.json({ limit: "1mb" }));
app.use(express.urlencoded({ extended: true }));

// ── Request logging ───────────────────────────────────────────────
app.use((req, _res, next) => {
  logger.debug(`${req.method} ${req.path}`, {
    ip:          req.ip,
    userAgent:   req.headers["user-agent"],
  });
  next();
});

// ── Health check ──────────────────────────────────────────────────
app.get("/health", (_req, res) => {
  res.json({ status: "ok", timestamp: Date.now() });
});

// ── Routes ────────────────────────────────────────────────────────
app.use("/auth",          authRoutes);
app.use("/devices",       deviceRoutes);
app.use("/sensors",       sensorRoutes);
app.use("/notifications", notificationRoutes);

// ── 404 handler ───────────────────────────────────────────────────
app.use((_req, res) => {
  res.status(404).json({ success: false, error: "Endpoint not found" });
});

// ── Global error handler (must be last) ──────────────────────────
app.use(errorHandler);

export default app;
