/**
 * services/auth.service.ts
 * ─────────────────────────────────────────────────────────────
 * Business logic for user registration, login, role assignment,
 * and FCM token management.
 *
 * The Flutter client calls Firebase Auth SDK directly for the
 * actual sign-in; these functions handle the server-side setup
 * (creating the DB profile, setting custom claims, etc.).
 */

import { auth, db } from "../config/firebase";
import { DB_PATHS, ROLES, UserRole } from "../config/constants";
import { nowMs, generateId, isValidEmail } from "../utils/helpers";
import { logger } from "../utils/logger";

// ── Types ────────────────────────────────────────────────────────

export interface RegisterPayload {
  uid:         string;   // Firebase Auth UID (from ID token)
  email:       string;
  displayName?: string;
  role?:       UserRole;
}

export interface UserProfile {
  uid:         string;
  email:       string;
  displayName: string;
  role:        UserRole;
  createdAt:   number;
  lastSeen:    number;
  fcmTokens:   Record<string, string>;
}

// ── Register / onboard a new user ───────────────────────────────

/**
 * Creates the user profile in Realtime DB and sets custom claims.
 * Called after the client has created an account via Firebase Auth SDK.
 */
export async function registerUser(payload: RegisterPayload): Promise<UserProfile> {
  if (!isValidEmail(payload.email)) {
    throw Object.assign(new Error("Invalid email address"), { statusCode: 400 });
  }

  const role: UserRole = payload.role || ROLES.USER;

  const profile: UserProfile = {
    uid:         payload.uid,
    email:       payload.email,
    displayName: payload.displayName || payload.email.split("@")[0],
    role,
    createdAt:   nowMs(),
    lastSeen:    nowMs(),
    fcmTokens:   {},
  };

  // Write profile to DB
  await db.ref(`${DB_PATHS.USERS}/${payload.uid}`).set(profile);

  // Persist role as a custom claim so DB rules can read it from the token
  await auth.setCustomUserClaims(payload.uid, { role });

  logger.info("User registered", { uid: payload.uid, role });
  return profile;
}

// ── Fetch user profile ───────────────────────────────────────────

export async function getUserProfile(uid: string): Promise<UserProfile | null> {
  const snap = await db.ref(`${DB_PATHS.USERS}/${uid}`).once("value");
  return snap.val() as UserProfile | null;
}

// ── Update last seen ─────────────────────────────────────────────

export async function touchLastSeen(uid: string): Promise<void> {
  await db.ref(`${DB_PATHS.USERS}/${uid}/lastSeen`).set(nowMs());
}

// ── FCM Token management ─────────────────────────────────────────

/**
 * Registers or refreshes an FCM token for a user.
 * Each device (phone, tablet) has its own token keyed by a stable
 * token ID that the client generates and stores locally.
 */
export async function upsertFcmToken(uid: string, token: string): Promise<void> {
  // Use the last 12 chars of the token as a stable key
  const tokenKey = token.slice(-12).replace(/[^a-zA-Z0-9]/g, "_");
  await db.ref(`${DB_PATHS.USERS}/${uid}/fcmTokens/${tokenKey}`).set(token);
  logger.debug("FCM token upserted", { uid, tokenKey });
}

/**
 * Removes a specific FCM token (e.g. on logout).
 */
export async function removeFcmToken(uid: string, token: string): Promise<void> {
  const tokenKey = token.slice(-12).replace(/[^a-zA-Z0-9]/g, "_");
  await db.ref(`${DB_PATHS.USERS}/${uid}/fcmTokens/${tokenKey}`).remove();
  logger.debug("FCM token removed", { uid, tokenKey });
}

/**
 * Returns all FCM tokens registered for a user.
 */
export async function getFcmTokens(uid: string): Promise<string[]> {
  const snap = await db.ref(`${DB_PATHS.USERS}/${uid}/fcmTokens`).once("value");
  const tokenMap = snap.val() as Record<string, string> | null;
  return tokenMap ? Object.values(tokenMap) : [];
}

/**
 * Returns FCM tokens for ALL users (used for broadcast notifications).
 */
export async function getAllUserTokens(): Promise<{ uid: string; tokens: string[] }[]> {
  const snap = await db.ref(DB_PATHS.USERS).once("value");
  const users = snap.val() as Record<string, UserProfile> | null;
  if (!users) return [];

  return Object.entries(users)
    .filter(([, u]) => u.fcmTokens && Object.keys(u.fcmTokens).length > 0)
    .map(([uid, u]) => ({
      uid,
      tokens: Object.values(u.fcmTokens),
    }));
}

// ── Role Management (admin only) ─────────────────────────────────

export async function setUserRole(uid: string, role: UserRole): Promise<void> {
  await db.ref(`${DB_PATHS.USERS}/${uid}/role`).set(role);
  await auth.setCustomUserClaims(uid, { role });
  logger.info("Role updated", { uid, role });
}

// ── Device token generation (for ESP32) ─────────────────────────

import * as jwt from "jsonwebtoken";

/**
 * Issues a long-lived JWT for an ESP32 device.
 * Store the returned token on the device's flash/NVS.
 */
export function issueDeviceToken(
  deviceId: string,
  expiresInDays = 365
): string {
  const secret = process.env.DEVICE_SECRET;
  if (!secret) throw new Error("DEVICE_SECRET not configured");

  return jwt.sign(
    { deviceId, jti: generateId(8) },
    secret,
    { expiresIn: `${expiresInDays}d` }
  );
}
