/**
 * api/sensor.routes.ts
 * ─────────────────────────────────────────────────────────────
 * REST endpoints consumed by the ESP32 firmware.
 *
 * POST /sensors/:deviceId/batch      — full multi-sensor upload
 * POST /sensors/:deviceId/water      — water level only
 * POST /sensors/:deviceId/rain       — rain status only
 * POST /sensors/:deviceId/temp       — temperature + humidity
 * GET  /sensors/:deviceId/history/:type — sensor history
 */

import { Router, Request, Response } from "express";
import {
  ingestSensorBatch,
  ingestWaterLevel,
  ingestRainStatus,
  ingestTemperatureHumidity,
  getSensorHistory,
} from "../services/sensor.service";
import {
  verifyDeviceToken,
  verifyFirebaseToken,
} from "../middleware/auth.middleware";
import { asyncHandler } from "../middleware/error.middleware";
import { successResponse, errorResponse } from "../utils/helpers";

const router = Router();

// ─────────────────────────────────────────────────────────────
// ESP32 endpoints — authenticated with device JWT
// ─────────────────────────────────────────────────────────────

/**
 * POST /sensors/:deviceId/batch
 *
 * Single POST that uploads all sensor readings at once.
 * Recommended over individual endpoints to reduce round-trips.
 *
 * Body (JSON):
 * {
 *   "waterLevel": { "value": 45, "unit": "%" },
 *   "rain":       { "rawValue": 2800, "isRaining": false },
 *   "tempHumidity": { "temperature": 28.5, "humidity": 72 }
 * }
 */
router.post(
  "/:deviceId/batch",
  verifyDeviceToken,
  asyncHandler(async (req: Request, res: Response) => {
    const { deviceId } = req.params;

    // Ensure the token belongs to THIS device
    if (req.user?.deviceId !== deviceId) {
      res.status(403).json(errorResponse("Token does not match device"));
      return;
    }

    const { waterLevel, rain, tempHumidity } = req.body as {
      waterLevel?:  { value?: number; rawValue?: number; unit?: string };
      rain?:        { rawValue: number; isRaining?: boolean };
      tempHumidity?: { temperature: number; humidity: number };
    };

    if (!waterLevel && !rain && !tempHumidity) {
      res.status(400).json(errorResponse("At least one sensor payload is required"));
      return;
    }

    await ingestSensorBatch({ deviceId, waterLevel, rain, tempHumidity });

    res.json(successResponse({ deviceId }, "Sensor batch received"));
  })
);

/**
 * POST /sensors/:deviceId/water
 */
router.post(
  "/:deviceId/water",
  verifyDeviceToken,
  asyncHandler(async (req: Request, res: Response) => {
    const { deviceId } = req.params;
    if (req.user?.deviceId !== deviceId) {
      res.status(403).json(errorResponse("Token does not match device"));
      return;
    }

    const reading = await ingestWaterLevel({ ...req.body, deviceId });
    res.json(successResponse(reading));
  })
);

/**
 * POST /sensors/:deviceId/rain
 */
router.post(
  "/:deviceId/rain",
  verifyDeviceToken,
  asyncHandler(async (req: Request, res: Response) => {
    const { deviceId } = req.params;
    if (req.user?.deviceId !== deviceId) {
      res.status(403).json(errorResponse("Token does not match device"));
      return;
    }

    const reading = await ingestRainStatus({ ...req.body, deviceId });
    res.json(successResponse(reading));
  })
);

/**
 * POST /sensors/:deviceId/temp
 */
router.post(
  "/:deviceId/temp",
  verifyDeviceToken,
  asyncHandler(async (req: Request, res: Response) => {
    const { deviceId } = req.params;
    if (req.user?.deviceId !== deviceId) {
      res.status(403).json(errorResponse("Token does not match device"));
      return;
    }

    const { temperature, humidity } = req.body as {
      temperature?: number;
      humidity?:    number;
    };

    if (temperature === undefined || humidity === undefined) {
      res.status(400).json(errorResponse("`temperature` and `humidity` required"));
      return;
    }

    await ingestTemperatureHumidity({ deviceId, temperature, humidity });
    res.json(successResponse({ deviceId, temperature, humidity }));
  })
);

// ─────────────────────────────────────────────────────────────
// Mobile/web read endpoints — authenticated with Firebase ID token
// ─────────────────────────────────────────────────────────────

/**
 * GET /sensors/:deviceId/history/:type?limit=100
 */
router.get(
  "/:deviceId/history/:type",
  verifyFirebaseToken,
  asyncHandler(async (req: Request, res: Response) => {
    const { deviceId, type } = req.params;
    const limit = Math.min(Number(req.query.limit) || 100, 500);

    const allowedTypes = ["water_level", "rain_status", "temperature", "humidity"];
    if (!allowedTypes.includes(type)) {
      res.status(400).json(errorResponse(`type must be one of: ${allowedTypes.join(", ")}`));
      return;
    }

    const history = await getSensorHistory(deviceId, type, limit);
    res.json(successResponse({ deviceId, type, history }));
  })
);

export default router;
