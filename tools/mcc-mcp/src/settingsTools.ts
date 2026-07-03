import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { result } from "./mcpResult.js";
import { HostPortInput } from "./toolSchemas.js";
import { ToolContext, openSettingsPanel } from "./tools.js";

export function registerSettingsTools(server: McpServer, ctx: ToolContext) {
  server.registerTool(
    "mcc_open_settings_panel",
    {
      title: "Open settings panel",
      description: "Open a settings bottom sheet by key: health_data, import_tools, theme, language, maintenance, support, or null to close.",
      inputSchema: {
        sheet: z.string().nullable(),
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ sheet, hostPort }) => result(await openSettingsPanel(ctx, sheet, hostPort)),
  );
}
