import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { result } from "./mcpResult.js";
import { HostPortInput } from "./toolSchemas.js";
import {
  ConnectInput,
  ToolContext,
  connectBridge,
  listDevices,
  logcat,
  navigate,
  runSmoke,
  screenshot,
  state,
} from "./tools.js";

export function registerDeviceTools(server: McpServer, ctx: ToolContext) {
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
      inputSchema: { hostPort: HostPortInput },
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
        hostPort: HostPortInput,
      },
      annotations: { readOnlyHint: false, idempotentHint: true, openWorldHint: false },
    },
    async ({ screen, hostPort }) => result(await navigate(ctx, screen, hostPort)),
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
}
