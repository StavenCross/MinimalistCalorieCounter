#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import {
  ConnectInput,
  createToolContext,
  DEFAULT_HOST_PORT,
  connectBridge,
  deleteHealthRange,
  listDevices,
  logcat,
  navigate,
  openSettingsPanel,
  quickImportCommit,
  quickImportPreview,
  readHealthDay,
  runSmoke,
  screenshot,
  selectMealsDate,
  state,
} from "./tools.js";

const ctx = createToolContext();
const server = new McpServer({
  name: "minimalist-calorie-counter",
  version: "0.1.0",
});

function result(data: unknown) {
  return {
    content: [{ type: "text" as const, text: JSON.stringify(data, null, 2) }],
    structuredContent: { data },
  };
}

server.registerTool(
  "mcc_list_devices",
  {
    title: "List Android devices",
    description: "List adb devices available for Minimalist Calorie Counter automation.",
    inputSchema: {},
    annotations: { readOnlyHint: true, openWorldHint: false },
  },
  async () => result(await listDevices(ctx)),
);

server.registerTool(
  "mcc_connect",
  {
    title: "Connect to app debug bridge",
    description: "Forward a local TCP port to the app's debug automation bridge and verify /health.",
    inputSchema: ConnectInput.shape,
    annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
  },
  async (input) => result(await connectBridge(ctx, input)),
);

server.registerTool(
  "mcc_state",
  {
    title: "Read app state",
    description: "Read current app state from the debug automation bridge.",
    inputSchema: { hostPort: z.number().int().positive().default(DEFAULT_HOST_PORT) },
    annotations: { readOnlyHint: true, openWorldHint: false },
  },
  async ({ hostPort }) => result(await state(ctx, hostPort)),
);

server.registerTool(
  "mcc_navigate",
  {
    title: "Navigate app",
    description: "Navigate to quick_add, meals, settings, database, or day through the app bridge.",
    inputSchema: {
      screen: z.string(),
      hostPort: z.number().int().positive().default(DEFAULT_HOST_PORT),
    },
    annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
  },
  async ({ screen, hostPort }) => result(await navigate(ctx, screen, hostPort)),
);

const QuickImportInput = {
  text: z.string().optional(),
  dateTime: z.string().optional().describe("Local ISO date-time, for example 2026-07-02T12:30:00"),
  snackOverride: z.boolean().optional(),
  addDatabase: z.boolean().optional(),
  addDay: z.boolean().optional(),
  writeHealthConnect: z.boolean().optional(),
  hostPort: z.number().int().positive().default(DEFAULT_HOST_PORT),
};

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
      hostPort: z.number().int().positive().default(DEFAULT_HOST_PORT),
    },
    annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
  },
  async ({ date, hostPort }) => result(await selectMealsDate(ctx, date, hostPort)),
);

server.registerTool(
  "mcc_health_read_day",
  {
    title: "Read Health Connect day",
    description: "Ask the app to read app-owned Health Connect nutrition records for one date.",
    inputSchema: {
      date: z.string().describe("ISO date, for example 2026-07-02"),
      hostPort: z.number().int().positive().default(DEFAULT_HOST_PORT),
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
      hostPort: z.number().int().positive().default(DEFAULT_HOST_PORT),
    },
    annotations: { readOnlyHint: false, destructiveHint: true, idempotentHint: true, openWorldHint: false },
  },
  async ({ startDate, endDate, hostPort }) => result(await deleteHealthRange(ctx, startDate, endDate, hostPort)),
);

server.registerTool(
  "mcc_open_settings_panel",
  {
    title: "Open settings panel",
    description: "Open a settings bottom sheet by key: health_data, import_tools, maintenance, support, or null to close.",
    inputSchema: {
      sheet: z.string().nullable(),
      hostPort: z.number().int().positive().default(DEFAULT_HOST_PORT),
    },
    annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
  },
  async ({ sheet, hostPort }) => result(await openSettingsPanel(ctx, sheet, hostPort)),
);

server.registerTool(
  "mcc_screenshot",
  {
    title: "Capture device screenshot",
    description: "Capture a screenshot from an adb device and return the local PNG path.",
    inputSchema: {
      deviceSerial: z.string(),
      outputPath: z.string().optional(),
    },
    annotations: { readOnlyHint: true, openWorldHint: false },
  },
  async ({ deviceSerial, outputPath }) => result(await screenshot(ctx, deviceSerial, outputPath)),
);

server.registerTool(
  "mcc_logcat",
  {
    title: "Read app logcat",
    description: "Read recent logcat lines filtered to the debug app process when possible.",
    inputSchema: {
      deviceSerial: z.string(),
      packageName: z.string().default("com.makstuff.minimalistcaloriecounter.debug"),
      lines: z.number().int().positive().max(2000).default(300),
    },
    annotations: { readOnlyHint: true, openWorldHint: false },
  },
  async ({ deviceSerial, packageName, lines }) => result(await logcat(ctx, deviceSerial, packageName, lines)),
);

server.registerTool(
  "mcc_run_smoke",
  {
    title: "Run MCC smoke script",
    description: "Run one of the local Android smoke scripts.",
    inputSchema: {
      script: z.enum(["fold", "fold_health", "automation"]),
    },
    annotations: { readOnlyHint: false, openWorldHint: false },
  },
  async ({ script }) => result(await runSmoke(ctx, script)),
);

await server.connect(new StdioServerTransport());
