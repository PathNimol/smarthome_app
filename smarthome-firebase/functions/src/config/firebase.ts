/**
 * config/firebase.ts
 * ─────────────────────────────────────────────────────────────
 * Initialises the Firebase Admin SDK once and exports the
 * shared admin, db, auth, and messaging instances used across
 * all services and Cloud Functions.
 */

import * as admin from "firebase-admin";
import { getDatabase } from "firebase-admin/database";
import { getAuth }     from "firebase-admin/auth";
import { getMessaging } from "firebase-admin/messaging";
import * as functions  from "firebase-functions";

// ── Initialise only once (guards against hot-reload duplicates) ──
if (!admin.apps.length) {
  const serviceAccountBase64 =
    process.env.FIREBASE_SERVICE_ACCOUNT_BASE64 ||
    functions.config().firebase?.service_account_base64;

  if (serviceAccountBase64) {
    // Production: decode the base64-encoded service-account JSON
    const serviceAccount = JSON.parse(
      Buffer.from(serviceAccountBase64, "base64").toString("utf8")
    );
    admin.initializeApp({
      credential:  admin.credential.cert(serviceAccount),
      databaseURL: process.env.FIREBASE_DATABASE_URL ||
                   functions.config().firebase?.database_url,
    });
  } else {
    // Local emulator / CI: use default application credentials
    admin.initializeApp({
      databaseURL: process.env.FIREBASE_DATABASE_URL ||
                   "http://localhost:9000?ns=smarthome-dev",
    });
  }
}

export const firebaseAdmin = admin;
export const db            = getDatabase();
export const auth          = getAuth();
export const messaging     = getMessaging();
