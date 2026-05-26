/**
 * api/auth.routes.ts
 * ─────────────────────────────────────────────────────────────
 * REST endpoints for user management and FCM token registration.
 *
 * POST /auth/register      — create DB profile after Firebase signup
 * POST /auth/fcm-token     — register / refresh FCM token
 * DELETE /auth/fcm-token   — remove FCM token on logout
 * GET  /auth/profile       — get current user profile
 * PUT  /auth/role          — set user role (admin only)
 */

import { Router, Request, Response } from "express";
import {
  registerUser,
  getUserProfile,
  upsertFcmToken,
  removeFcmToken,
  setUserRole,
} from "../services/auth.service";
import { verifyFirebaseToken, requireRole } from "../middleware/auth.middleware";
import { asyncHandler }  from "../middleware/error.middleware";
import { ROLES }         from "../config/constants";
import { successResponse, errorResponse } from "../utils/helpers";
import { logActivity }   from "../services/activity.service";
import { logger }        from "../utils/logger";

const router = Router();

// ── POST /auth/register ─────────────────────────────────────────

router.post(
  "/register",
  verifyFirebaseToken,
  asyncHandler(async (req: Request, res: Response) => {
    const { displayName } = req.body as { displayName?: string };

    const profile = await registerUser({
      uid:         req.user!.uid,
      email:       req.user!.email!,
      displayName,
    });

    await logActivity("user_registered", req.user!.uid, "", { email: profile.email });

    logger.info("User registered", { uid: profile.uid });
    res.status(201).json(successResponse(profile, "User registered successfully"));
  })
);

// ── GET /auth/profile ───────────────────────────────────────────

router.get(
  "/profile",
  verifyFirebaseToken,
  asyncHandler(async (req: Request, res: Response) => {
    const profile = await getUserProfile(req.user!.uid);
    if (!profile) {
      res.status(404).json(errorResponse("User profile not found — call /register first"));
      return;
    }
    res.json(successResponse(profile));
  })
);

// ── POST /auth/fcm-token ────────────────────────────────────────

router.post(
  "/fcm-token",
  verifyFirebaseToken,
  asyncHandler(async (req: Request, res: Response) => {
    const { token } = req.body as { token?: string };
    if (!token || typeof token !== "string") {
      res.status(400).json(errorResponse("Missing `token` in request body"));
      return;
    }
    await upsertFcmToken(req.user!.uid, token);
    res.json(successResponse(null, "FCM token registered"));
  })
);

// ── DELETE /auth/fcm-token ──────────────────────────────────────

router.delete(
  "/fcm-token",
  verifyFirebaseToken,
  asyncHandler(async (req: Request, res: Response) => {
    const { token } = req.body as { token?: string };
    if (!token) {
      res.status(400).json(errorResponse("Missing `token` in request body"));
      return;
    }
    await removeFcmToken(req.user!.uid, token);
    res.json(successResponse(null, "FCM token removed"));
  })
);

// ── PUT /auth/role (admin only) ─────────────────────────────────

router.put(
  "/role",
  verifyFirebaseToken,
  requireRole(ROLES.ADMIN),
  asyncHandler(async (req: Request, res: Response) => {
    const { targetUid, role } = req.body as { targetUid?: string; role?: string };

    if (!targetUid || !role) {
      res.status(400).json(errorResponse("targetUid and role are required"));
      return;
    }
    if (![ROLES.ADMIN, ROLES.USER, ROLES.VIEWER].includes(role as never)) {
      res.status(400).json(errorResponse("Invalid role"));
      return;
    }

    await setUserRole(targetUid, role as never);
    await logActivity("role_changed", req.user!.uid, "", { targetUid, role });

    res.json(successResponse(null, `Role updated to ${role}`));
  })
);

export default router;
