/**
 * api/notification.routes.ts
 * ─────────────────────────────────────────────────────────────
 * Notification inbox management for the Flutter app.
 *
 * GET  /notifications          — get user's notification inbox
 * PUT  /notifications/:id/read — mark notification as read
 * POST /notifications/test     — send a test push (dev only)
 */

import { Router, Request, Response } from "express";
import { db }                   from "../config/firebase";
import { DB_PATHS }             from "../config/constants";
import { markNotificationRead, sendToUser } from "../notifications/fcm.service";
import { verifyFirebaseToken, requireRole } from "../middleware/auth.middleware";
import { asyncHandler }         from "../middleware/error.middleware";
import { ROLES }                from "../config/constants";
import { successResponse, errorResponse } from "../utils/helpers";

const router = Router();
router.use(verifyFirebaseToken);

// ── GET /notifications ──────────────────────────────────────────

router.get(
  "/",
  asyncHandler(async (req: Request, res: Response) => {
    const snap = await db
      .ref(`${DB_PATHS.NOTIFICATIONS}/${req.user!.uid}`)
      .orderByChild("timestamp")
      .limitToLast(50)
      .once("value");

    const val = snap.val() as Record<string, unknown> | null;
    const notifications = val
      ? Object.values(val).sort((a: any, b: any) => b.timestamp - a.timestamp)
      : [];

    res.json(successResponse(notifications));
  })
);

// ── PUT /notifications/:id/read ─────────────────────────────────

router.put(
  "/:notifId/read",
  asyncHandler(async (req: Request, res: Response) => {
    await markNotificationRead(req.user!.uid, req.params.notifId);
    res.json(successResponse(null, "Notification marked as read"));
  })
);

// ── POST /notifications/test (admin / dev only) ─────────────────

router.post(
  "/test",
  requireRole(ROLES.ADMIN),
  asyncHandler(async (req: Request, res: Response) => {
    if (process.env.NODE_ENV === "production") {
      res.status(403).json(errorResponse("Test endpoint disabled in production"));
      return;
    }

    await sendToUser(req.user!.uid, {
      type:     "test",
      title:    "🧪 Test Notification",
      body:     "If you see this, FCM is working correctly!",
      deviceId: "test_device",
    });

    res.json(successResponse(null, "Test notification sent"));
  })
);

export default router;
