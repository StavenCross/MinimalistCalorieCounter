import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { result } from "./mcpResult.js";
import { HostPortInput, QuickImportInput } from "./toolSchemas.js";
import {
  ToolContext,
  quickImportCommit,
  quickImportPreview,
  selectMealsDate,
} from "./tools.js";

export function registerQuickImportTools(server: McpServer, ctx: ToolContext) {
  server.registerTool(
    "mcc_quick_import_preview",
    {
      title: "Preview Quick Add import",
      description: "Parse a ChatGPT nutrition blurb through the app's real Quick Add parser without writing.",
      inputSchema: QuickImportInput,
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ hostPort, ...input }) => result(await quickImportPreview(ctx, input, hostPort)),
  );

  server.registerTool(
    "mcc_quick_import_commit",
    {
      title: "Commit Quick Add import",
      description: "Commit a Quick Add import through the app's real write path. Can write local DB/day rows and Health Connect.",
      inputSchema: QuickImportInput,
      annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: false },
    },
    async ({ hostPort, ...input }) => result(await quickImportCommit(ctx, input, hostPort)),
  );

  server.registerTool(
    "mcc_select_meals_date",
    {
      title: "Select Meals date",
      description: "Select a date on the Meals screen.",
      inputSchema: {
        date: z.string().describe("ISO date, for example 2026-07-02"),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ date, hostPort }) => result(await selectMealsDate(ctx, date, hostPort)),
  );
}
