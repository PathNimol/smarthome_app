/**
 * api/device.routes.ts
 * ─────────────────────────────────────────────────────────────
 * REST endpoints for device registration and relay control.
 *
 * POST   /devices              — register a new device (admin)
 * GET    /devices              — list all devices
 * GET    /devices/:id/relays   — get relay states
 * PUT    /devices/:id/relays/:relayId — toggle relay
 */

import { Router, Request, Response } from "express";
import {
  registerDevice,
  getAllDevices,
  getRelayStates,
  setRelayState,
} from "../services/device.service";
import { verifyFirebaseToken, requireRole } from "../middleware/auth.middleware";
import { asyncHandler } from "../middleware/error.middleware";
import { ROLES }        from "../config/constants";
import { successResponse, errorResponse } from "../utils/helpers";
import { logActivity }  from "../services/activity.service";

const router = Router();

// All device routes require authentication
router.use(verifyFirebaseToken);

// ── POST /devices ───────────────────────────────────────────────

router.post(
  "/",
  requireRole(ROLES.ADMIN),
  asyncHandler(async (req: Request, res: Response) => {
    const { name, type, location } = req.body as {
      name?: string;
      type?: string;
      location?: string;
    };

    if (!name || !type) {
      res.status(400).json(errorResponse("`name` and `type` are required"));
      return;
    }

    const result = await registerDevice({
      name,
      type,
      location: location || "unknown",
      ownerId:  req.user!.uid,
    });

    await logActivity("device_registered", req.user!.uid, result.device.id, {
      name, type,
    });

    // Return the token once — the admin must flash it onto the ESP32
    res.status(201).json(successResponse({
      device: result.device,
      token:  result.token,   // ⚠ show only once
    }, "Device registered. Store the token securely — it will not be shown again."));
  })
);

// ── GET /devices ────────────────────────────────────────────────

router.get(
  "/",
  asyncHandler(async (_req: Request, res: Response) => {
    const devices = await getAllDevices();
    res.json(successResponse(devices));
  })
);

// ── GET /devices/:id/relays ─────────────────────────────────────

router.get(
  "/:deviceId/relays",
  asyncHandler(async (req: Request, res: Response) => {
    const { deviceId } = req.params;
    const relays = await getRelayStates(deviceId);
    res.json(successResponse(relays));
  })
);

// ── PUT /devices/:id/relays/:relayId ────────────────────────────

router.put(
  "/:deviceId/relays/:relayId",
  asyncHandler(async (req: Request, res: Response) => {
    const { deviceId, relayId } = req.params;
    const { state } = req.body as { state?: boolean };

    if (typeof state !== "boolean") {
      res.status(400).json(errorResponse("`state` must be a boolean"));
      return;
    }

    const updated = await setRelayState(deviceId, relayId, state, req.user!.uid);

    await logActivity("relay_set", req.user!.uid, deviceId, { relayId, state });

    res.json(successResponse(updated, `Relay ${relayId} set to ${state}`));
  })
);

export default router;
