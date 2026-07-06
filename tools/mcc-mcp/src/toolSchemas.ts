import { z } from "zod";
import { DEFAULT_HOST_PORT } from "./tools.js";

export const HostPortInput = z.number().int().positive().default(DEFAULT_HOST_PORT);

export const QuickImportInput = {
  text: z.string().optional(),
  dateTime: z.string().optional().describe("Local ISO date-time, for example 2026-07-02T12:30:00"),
  mealType: z.enum(["Breakfast", "Lunch", "Dinner", "Snack"]).optional(),
  snackOverride: z.boolean().optional(),
  hostPort: HostPortInput,
};

export const GoalsProfileInput = {
  birthday: z.string().optional(),
  sex: z.enum(["Male", "Female"]).optional(),
  heightCm: z.number().nullable().optional(),
  weightKg: z.number().nullable().optional(),
  bodyFatPercent: z.number().nullable().optional(),
  leanMassKg: z.number().nullable().optional(),
  activityLevel: z.enum(["Sedentary", "LightlyActive", "ModeratelyActive", "VeryActive", "ExtraActive"]).optional(),
  weightLossTarget: z.enum(["Maintain", "HalfPound", "OnePound", "OneAndHalfPounds", "TwoPounds"]).optional(),
  targets: z.object({
    calories: z.number().nullable().optional(),
    protein: z.number().nullable().optional(),
    carbs: z.number().nullable().optional(),
    fat: z.number().nullable().optional(),
    fiber: z.number().nullable().optional(),
  }).optional(),
  hostPort: HostPortInput,
};
