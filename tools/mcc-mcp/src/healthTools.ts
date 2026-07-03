import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { result } from "./mcpResult.js";
import { HostPortInput } from "./toolSchemas.js";
import {
  ToolContext,
  deleteHealthRange,
  exportHealthRange,
  readHealthDay,
} from "./tools.js";

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
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, destructiveHint: true, idempotentHint: true, openWorldHint: false },
    },
    async ({ startDate, endDate, hostPort }) => result(await deleteHealthRange(ctx, startDate, endDate, hostPort)),
  );

  server.registerTool(
    "mcc_health_export_range",
    {
      title: "Export Health Connect range",
      description: "Export all readable Health Connect records in an inclusive date range to a CSV in device Downloads.",
      inputSchema: {
        startDate: z.string(),
        endDate: z.string(),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: false },
    },
    async ({ startDate, endDate, hostPort }) => result(await exportHealthRange(ctx, startDate, endDate, hostPort)),
  );
}
