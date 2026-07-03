import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { result } from "./mcpResult.js";
import { GoalsProfileInput, HostPortInput } from "./toolSchemas.js";
import {
  ToolContext,
  applyGoalsRecommendation,
  goalsState,
  recalculateGoals,
  refreshGoalsHealthConnect,
  setGoalsMacro,
  setGoalsMeasurement,
  setGoalsProfile,
  setGoalsSettingsVisible,
  toggleGoalsMacroLock,
  toggleGoalsMeasurementLock,
} from "./tools.js";

export function registerGoalsTools(server: McpServer, ctx: ToolContext) {
  server.registerTool(
    "mcc_goals_state",
    {
      title: "Read Goals state",
      description: "Read the Goals tab state from the app debug bridge.",
      inputSchema: { hostPort: HostPortInput },
      annotations: { readOnlyHint: true, openWorldHint: false },
    },
    async ({ hostPort }) => result(await goalsState(ctx, hostPort)),
  );

  server.registerTool(
    "mcc_goals_settings",
    {
      title: "Open or close Goals settings",
      description: "Show or hide the Goals settings bottom sheet.",
      inputSchema: {
        visible: z.boolean().default(true),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ visible, hostPort }) => result(await setGoalsSettingsVisible(ctx, visible, hostPort)),
  );

  server.registerTool(
    "mcc_goals_set_profile",
    {
      title: "Set Goals profile",
      description: "Set one or more Goals profile values, including body metrics and activity preferences.",
      inputSchema: GoalsProfileInput,
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ hostPort, ...input }) => result(await setGoalsProfile(ctx, input, hostPort)),
  );

  server.registerTool(
    "mcc_goals_set_measurement",
    {
      title: "Set Goals measurement",
      description: "Set a single Goals measurement field.",
      inputSchema: {
        field: z.enum(["HeightCm", "WeightKg", "BodyFatPercent", "LeanMassKg"]),
        value: z.number().nullable(),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ field, value, hostPort }) => result(await setGoalsMeasurement(ctx, field, value, hostPort)),
  );

  server.registerTool(
    "mcc_goals_toggle_measurement_lock",
    {
      title: "Toggle Goals measurement lock",
      description: "Toggle the manual/Health Connect lock for a Goals measurement.",
      inputSchema: {
        field: z.enum(["HeightCm", "WeightKg", "BodyFatPercent", "LeanMassKg"]),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, idempotentHint: false, openWorldHint: false },
    },
    async ({ field, hostPort }) => result(await toggleGoalsMeasurementLock(ctx, field, hostPort)),
  );

  server.registerTool(
    "mcc_goals_set_macro",
    {
      title: "Set Goals macro target",
      description: "Set a single Goals macro target.",
      inputSchema: {
        macro: z.enum(["Calories", "Protein", "Carbs", "Fat", "Fiber"]),
        value: z.number().nullable(),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ macro, value, hostPort }) => result(await setGoalsMacro(ctx, macro, value, hostPort)),
  );

  server.registerTool(
    "mcc_goals_toggle_macro_lock",
    {
      title: "Toggle Goals macro lock",
      description: "Toggle the manual lock for a Goals macro target.",
      inputSchema: {
        macro: z.enum(["Calories", "Protein", "Carbs", "Fat", "Fiber"]),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, idempotentHint: false, openWorldHint: false },
    },
    async ({ macro, hostPort }) => result(await toggleGoalsMacroLock(ctx, macro, hostPort)),
  );

  server.registerTool(
    "mcc_goals_refresh_health_connect",
    {
      title: "Refresh Goals from Health Connect",
      description: "Ask the app to refresh unlocked Goals body measurements from Health Connect.",
      inputSchema: { hostPort: HostPortInput },
      annotations: { readOnlyHint: false, idempotentHint: false, openWorldHint: false },
    },
    async ({ hostPort }) => result(await refreshGoalsHealthConnect(ctx, hostPort)),
  );

  server.registerTool(
    "mcc_goals_recalculate",
    {
      title: "Recalculate Goals",
      description: "Generate a Goals recommendation from the current profile and macro locks.",
      inputSchema: { hostPort: HostPortInput },
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ hostPort }) => result(await recalculateGoals(ctx, hostPort)),
  );

  server.registerTool(
    "mcc_goals_apply_recommendation",
    {
      title: "Apply Goals recommendation",
      description: "Apply the pending Goals recommendation to current targets and history.",
      inputSchema: { hostPort: HostPortInput },
      annotations: { readOnlyHint: false, idempotentHint: false, openWorldHint: false },
    },
    async ({ hostPort }) => result(await applyGoalsRecommendation(ctx, hostPort)),
  );
}
