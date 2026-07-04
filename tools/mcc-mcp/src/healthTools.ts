import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { result } from "./mcpResult.js";
import { HostPortInput } from "./toolSchemas.js";
import {
  ToolContext,
  deleteHealthRange,
  exportHealthRange,
  previewHealthDeleteRange,
  readHealthDay,
  setHealthExportOptions,
} from "./tools.js";

const HealthExportMode = z.enum(["NutritionOnly", "NutritionAndGoals", "Full"]);
const HealthCleanupMode = z.enum(["HistoricalImports", "AddMeal", "AllAppNutrition"]);

export function registerHealthTools(server: McpServer, ctx: ToolContext) {
  server.registerTool(
    "mcc_health_read_day",
    {
      title: "Read Health Connect day",
      description: "Ask the app to read app-owned Health Connect nutrition records for one date.",
      inputSchema: {
        date: z.string().describe("ISO date, for example 2026-07-02"),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: true, openWorldHint: false },
    },
    async ({ date, hostPort }) => result(await readHealthDay(ctx, date, hostPort)),
  );

  server.registerTool(
    "mcc_health_delete_range",
    {
      title: "Delete Health Connect nutrition range",
      description: "Delete app-owned Health Connect nutrition records in an inclusive date range.",
      inputSchema: {
        startDate: z.string(),
        endDate: z.string(),
        mode: HealthCleanupMode.optional(),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, destructiveHint: true, idempotentHint: true, openWorldHint: false },
    },
    async ({ startDate, endDate, mode, hostPort }) => result(await deleteHealthRange(ctx, startDate, endDate, mode, hostPort)),
  );

  server.registerTool(
    "mcc_health_preview_delete_range",
    {
      title: "Preview Health Connect nutrition cleanup",
      description: "Preview matching app-owned Health Connect Nutrition records before deletion.",
      inputSchema: {
        startDate: z.string(),
        endDate: z.string(),
        mode: HealthCleanupMode.optional(),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: true, openWorldHint: false },
    },
    async ({ startDate, endDate, mode, hostPort }) =>
      result(await previewHealthDeleteRange(ctx, startDate, endDate, mode, hostPort)),
  );

  server.registerTool(
    "mcc_health_export_options",
    {
      title: "Set Health Connect export options",
      description: "Set Health Connect export mode and redaction before exporting.",
      inputSchema: {
        mode: HealthExportMode.optional(),
        redacted: z.boolean().optional(),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ mode, redacted, hostPort }) => result(await setHealthExportOptions(ctx, mode, redacted, hostPort)),
  );

  server.registerTool(
    "mcc_health_export_range",
    {
      title: "Export Health Connect range",
      description: "Export all readable Health Connect records in an inclusive date range to a CSV in device Downloads.",
      inputSchema: {
        startDate: z.string(),
        endDate: z.string(),
        mode: HealthExportMode.optional(),
        redacted: z.boolean().optional(),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: false },
    },
    async ({ startDate, endDate, mode, redacted, hostPort }) =>
      result(await exportHealthRange(ctx, startDate, endDate, mode, redacted, hostPort)),
  );
}
