/**
 * middleware/auth.middleware.ts
 * ─────────────────────────────────────────────────────────────
 * Express middleware for:
 *   1. Firebase ID-token verification (mobile/web clients)
 *   2. Device JWT verification (ESP32)
 *   3. Role-based access control
 */

import { Request, Response, NextFunction } from "express";
import * as jwt from "jsonwebtoken";
import { auth, db } from "../config/firebase";
import { DB_PATHS, ROLES, UserRole } from "../config/constants";
import { logger } from "../utils/logger";
import { errorResponse } from "../utils/helpers";

// ── Augment Express Request to carry decoded auth data ──────────
declare global {
  namespace Express {
    interface Request {
      user?: {
        uid:      string;
        email?:   string;
        role:     UserRole;
        deviceId?: string;    // set only for device requests
        isDevice?: boolean;
      };
    }
  }
}

// ── 1. Firebase ID Token (mobile / web) ─────────────────────────

/**
 * Verifies a Firebase ID token sent in the Authorization header.
 * Attaches `req.user` with uid, email, and role.
 *
 * Header: Authorization: Bearer <firebase-id-token>
 */
export const verifyFirebaseToken = async (
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> => {
  try {
    const header = req.headers.authorization;
    if (!header?.startsWith("Bearer ")) {
      res.status(401).json(errorResponse("Missing or malformed Authorization header"));
      return;
    }

    const idToken = header.split("Bearer ")[1];
    const decoded = await auth.verifyIdToken(idToken);

    // Fetch role from DB (custom claims are the canonical source)
    const role = (decoded.role as UserRole) || await getUserRole(decoded.uid);

    req.user = {
      uid:   decoded.uid,
      email: decoded.email,
      role,
    };
    next();
  } catch (err) {
    logger.warn("Firebase token verification failed", { error: String(err) });
    res.status(401).json(errorResponse("Invalid or expired token"));
  }
};

// ── 2. Device JWT (ESP32) ────────────────────────────────────────

/**
 * Verifies a device-signed JWT from an ESP32.
 * The ESP32 must include its token in the X-Device-Token header.
 * Token payload: { deviceId, iat, exp }
 */
export const verifyDeviceToken = async (
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> => {
  try {
    const token = req.headers["x-device-token"] as string | undefined;
    if (!token) {
      res.status(401).json(errorResponse("Missing X-Device-Token header"));
      return;
    }

    const secret = process.env.DEVICE_SECRET;
    if (!secret) throw new Error("DEVICE_SECRET not configured");

    const payload = jwt.verify(token, secret) as { deviceId: string };

    // Verify the device actually exists in the DB
    const deviceSnap = await db
      .ref(`${DB_PATHS.DEVICES}/${payload.deviceId}`)
      .once("value");

    if (!deviceSnap.exists()) {
      res.status(403).json(errorResponse("Device not registered"));
      return;
    }

    req.user = {
      uid:      payload.deviceId,
      role:     ROLES.USER,
      deviceId: payload.deviceId,
      isDevice: true,
    };
    next();
  } catch (err) {
    logger.warn("Device token verification failed", { error: String(err) });
    res.status(401).json(errorResponse("Invalid device token"));
  }
};

// ── 3. Role Guard ────────────────────────────────────────────────

/**
 * Returns middleware that allows only the specified roles.
 * Must be used AFTER verifyFirebaseToken.
 *
 * @example router.delete("/device", requireRole(ROLES.ADMIN), handler)
 */
export const requireRole = (...allowedRoles: UserRole[]) =>
  (req: Request, res: Response, next: NextFunction): void => {
    if (!req.user) {
      res.status(401).json(errorResponse("Unauthenticated"));
      return;
    }
    if (!allowedRoles.includes(req.user.role)) {
      res.status(403).json(errorResponse(
        `Access denied. Required role: ${allowedRoles.join(" or ")}`
      ));
      return;
    }
    next();
  };

// ── Helpers ──────────────────────────────────────────────────────

async function getUserRole(uid: string): Promise<UserRole> {
  try {
    const snap = await db.ref(`${DB_PATHS.USERS}/${uid}/role`).once("value");
    return (snap.val() as UserRole) || ROLES.VIEWER;
  } catch {
    return ROLES.VIEWER;
  }
}
