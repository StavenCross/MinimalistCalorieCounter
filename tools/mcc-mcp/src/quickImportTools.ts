import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { result } from "./mcpResult.js";
import { HostPortInput, QuickImportInput } from "./toolSchemas.js";
import {
  ToolContext,
  quickImportCommit,
  quickImportOutboxClear,
  quickImportPreview,
  quickImportRetry,
  selectMealsDate,
} from "./tools.js";

export function registerQuickImportTools(server: McpServer, ctx: ToolContext) {
  server.registerTool(
    "mcc_quick_import_preview",
    {
      title: "Preview Add Meal import",
      description: "Parse a ChatGPT nutrition blurb through the app's real Add Meal parser without writing.",
      inputSchema: QuickImportInput,
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ hostPort, ...input }) => result(await quickImportPreview(ctx, input, hostPort)),
  );

  server.registerTool(
    "mcc_quick_import_commit",
    {
      title: "Commit Add Meal import",
      description: "Commit an Add Meal import through the app's real write path with the app's fixed local backup and Health Connect destinations.",
      inputSchema: QuickImportInput,
      annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: false },
    },
    async ({ hostPort, ...input }) => result(await quickImportCommit(ctx, input, hostPort)),
  );

  server.registerTool(
    "mcc_quick_import_retry",
    {
      title: "Retry Add Meal Health Connect write",
      description: "Retry a failed or pending Add Meal Health Connect write from its outbox id.",
      inputSchema: {
        id: z.string().describe("Add Meal outbox id from the debug bridge state."),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false, openWorldHint: false },
    },
    async ({ id, hostPort }) => result(await quickImportRetry(ctx, id, hostPort)),
  );

  server.registerTool(
    "mcc_quick_import_outbox_clear",
    {
      title: "Clear Add Meal outbox rows",
      description: "Clear one Add Meal outbox row or all attention-needed rows for debug test setup.",
      inputSchema: {
        id: z.string().optional().describe("Optional outbox id. Omit to clear all matching rows."),
        attentionOnly: z.boolean().default(true).describe("When true, only pending, failed, or retrying rows are cleared."),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, destructiveHint: true, idempotentHint: true, openWorldHint: false },
    },
    async ({ id, attentionOnly, hostPort }) => result(await quickImportOutboxClear(ctx, id, attentionOnly, hostPort)),
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
