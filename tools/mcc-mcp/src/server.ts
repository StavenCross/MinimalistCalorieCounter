#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { registerDeviceTools } from "./deviceTools.js";
import { registerGoalsTools } from "./goalsTools.js";
import { registerHealthTools } from "./healthTools.js";
import { registerQuickImportTools } from "./quickImportTools.js";
import { registerSettingsTools } from "./settingsTools.js";
import { createToolContext } from "./tools.js";

const ctx = createToolContext();
const server = new McpServer({
  name: "minimalist-calorie-counter",
  version: "0.1.0",
});

registerDeviceTools(server, ctx);
registerQuickImportTools(server, ctx);
registerHealthTools(server, ctx);
registerSettingsTools(server, ctx);
registerGoalsTools(server, ctx);

await server.connect(new StdioServerTransport());
