// functions/index.js — Firebase Cloud Functions
// Deploy: firebase deploy --only functions

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.database();
const fcm = admin.messaging();
const firestore = admin.firestore();

// ─── Trigger: Water Level Change ─────────────────────────────────────────────
exports.onWaterLevelChange = functions.database
  .ref("/smarthome/sensors/water_level")
  .onUpdate(async (change) => {
    const newPct = change.after.val().percentage;
    const oldPct = change.before.val().percentage;

    // Only notify on threshold crossing (not every update)
    const wasLow = oldPct < 20;
    const isLow = newPct < 20;
    const wasOverflow = oldPct >= 90;
    const isOverflow = newPct >= 90;

    if (!wasLow && isLow) {
      await sendAlert({
        type: "low_water",
        title: "Water Level Low!",
        message: `⚠️ Water Level Low (${newPct}%)! Please refill the tank soon.`,
        data: { sensorValue: String(newPct) },
      });
    }

    if (!wasOverflow && isOverflow) {
      await sendAlert({
        type: "overflow",
        title: "Tank Almost Full!",
        message: `🚨 Tank Almost Full (${newPct}%)! Turn off water supply.`,
        data: { sensorValue: String(newPct) },
      });
    }
  });

// ─── Trigger: Rain Status Change ─────────────────────────────────────────────
exports.onRainStatusChange = functions.database
  .ref("/smarthome/sensors/rain/isRaining")
  .onUpdate(async (change) => {
    const wasRaining = change.before.val();
    const isRaining = change.after.val();

    if (!wasRaining && isRaining) {
      const intensity = (await db.ref("/smarthome/sensors/rain/intensity").once("value")).val();
      await sendAlert({
        type: "rain_alert",
        title: "Rain Detected!",
        message: `⚠️ Rain Detected! Intensity: ${intensity}. Please bring your clothes inside.`,
        data: { intensity },
      });

      // Auto-trigger LED light ON when rain detected
      await db.ref("/smarthome/devices/led_light").update({
        state: "ON",
        lastChanged: Date.now(),
        controlledBy: "auto",
      });
    }
  });

// ─── Trigger: Device State Change ────────────────────────────────────────────
exports.onDeviceChange = functions.database
  .ref("/smarthome/devices/{deviceId}/state")
  .onUpdate(async (change, context) => {
    const deviceId = context.params.deviceId;
    const newState = change.after.val();
    const label = deviceId === "fan" ? "Fan" : "LED Light";

    await saveNotificationToFirestore({
      type: newState === "ON" ? "device_on" : "device_off",
      title: `${label} turned ${newState}`,
      message: `${label} turned ${newState}.`,
      metadata: { deviceId },
    });
  });

// ─── Helpers ─────────────────────────────────────────────────────────────────

async function sendAlert({ type, title, message, data = {} }) {
  // Save to Firestore (all users — in production filter by userId)
  await saveNotificationToFirestore({ type, title, message, metadata: data });

  // Get all FCM tokens from Firestore (store them on login)
  const tokensSnap = await firestore.collection("fcm_tokens").get();
  const tokens = tokensSnap.docs.map((d) => d.data().token).filter(Boolean);

  if (tokens.length === 0) return;

  await fcm.sendEachForMulticast({
    tokens,
    notification: { title, body: message },
    data: { type, ...data },
    android: {
      priority: "high",
      notification: { channelId: getChannelId(type) },
    },
  });
}

async function saveNotificationToFirestore({ type, title, message, metadata = {} }) {
  await firestore.collection("notifications").add({
    type,
    title,
    message,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
    isRead: false,
    userId: "global", // Replace with actual userId in multi-user setup
    metadata,
  });
}

function getChannelId(type) {
  if (type.includes("rain")) return "rain_alerts";
  if (type.includes("water") || type.includes("overflow")) return "water_alerts";
  return "device_status";
}
