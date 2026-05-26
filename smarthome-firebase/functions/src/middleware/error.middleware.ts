/**
 * middleware/error.middleware.ts
 * ─────────────────────────────────────────────────────────────
 * Global Express error handler.  Must be registered LAST in the
 * app middleware chain.
 */

import { Request, Response, NextFunction } from "express";
import { logger } from "../utils/logger";

export interface AppError extends Error {
  statusCode?: number;
  code?:       string;
}

export const errorHandler = (
  err: AppError,
  req: Request,
  res: Response,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _next: NextFunction
): void => {
  const statusCode = err.statusCode || 500;
  const message    = err.message    || "Internal server error";

  logger.error("Unhandled error", {
    statusCode,
    message,
    code:   err.code,
    method: req.method,
    path:   req.path,
    stack:  err.stack,
  });

  res.status(statusCode).json({
    success: false,
    error:   message,
    ...(process.env.NODE_ENV !== "production" && { stack: err.stack }),
  });
};

/** Wraps an async route handler to forward errors to next(). */
export const asyncHandler =
  (fn: (req: Request, res: Response, next: NextFunction) => Promise<void>) =>
  (req: Request, res: Response, next: NextFunction): void => {
    fn(req, res, next).catch(next);
  };
