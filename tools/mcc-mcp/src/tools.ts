import { mkdtemp } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { z } from "zod";
import { AdbClient, CommandRunner, defaultRunner } from "./adb.js";
import { MccBridgeClient } from "./bridge.js";

export const DEFAULT_APP_PACKAGE = "com.makstuff.minimalistcaloriecounter.debug";
export const DEFAULT_DEVICE_PORT = 8765;
export const DEFAULT_HOST_PORT = 18765;
export const REPO_ROOT = resolve(new URL("../../..", import.meta.url).pathname);

export interface ToolContext {
  adb: AdbClient;
  run: CommandRunner;
  bridgeFor(port?: number): BridgeClient;
}

export interface BridgeClient {
  get(path: string): Promise<unknown>;
  post(path: string, body: Record<string, unknown>): Promise<unknown>;
}

export function createToolContext(run: CommandRunner = defaultRunner): ToolContext {
  return {
    adb: new AdbClient(run),
    run,
    bridgeFor: (port = DEFAULT_HOST_PORT) => new MccBridgeClient({ port }),
  };
}

export const ConnectInput = z.object({
  deviceSerial: z.string().describe("adb device serial, for example emulator-5554"),
  hostPort: z.number().int().positive().default(DEFAULT_HOST_PORT),
  devicePort: z.number().int().positive().default(DEFAULT_DEVICE_PORT),
});

export async function connectBridge(ctx: ToolContext, input: z.infer<typeof ConnectInput>) {
  await ctx.adb.forward(input.deviceSerial, input.hostPort, input.devicePort);
  return ctx.bridgeFor(input.hostPort).get("/health");
}

export async function listDevices(ctx: ToolContext) {
  return ctx.adb.listDevices();
}

export async function state(ctx: ToolContext, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).get("/state");
}

export async function navigate(ctx: ToolContext, screen: string, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/navigate", { screen });
}

export async function quickImportPreview(
  ctx: ToolContext,
  input: Record<string, unknown>,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/quick-import/preview", input);
}

export async function quickImportCommit(
  ctx: ToolContext,
  input: Record<string, unknown>,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/quick-import/commit", input);
}

export async function quickImportRetry(ctx: ToolContext, id: string, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/quick-import/retry", { id });
}

export async function quickImportOutboxClear(
  ctx: ToolContext,
  id: string | undefined,
  attentionOnly = true,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/quick-import/outbox/clear", { id, attentionOnly });
}

export async function selectMealsDate(ctx: ToolContext, date: string, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/meals/select-date", { date });
}

export async function readHealthDay(ctx: ToolContext, date: string, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/health-connect/read-day", { date });
}

export async function deleteHealthRange(
  ctx: ToolContext,
  startDate: string,
  endDate: string,
  mode?: string,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/health-connect/delete-range", { startDate, endDate, mode });
}

export async function previewHealthDeleteRange(
  ctx: ToolContext,
  startDate: string,
  endDate: string,
  mode?: string,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/health-connect/preview-delete-range", { startDate, endDate, mode });
}

export async function setHealthExportOptions(
  ctx: ToolContext,
  mode?: string,
  redacted?: boolean,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/health-connect/export-options", { mode, redacted });
}

export async function exportHealthRange(
  ctx: ToolContext,
  startDate: string,
  endDate: string,
  mode?: string,
  redacted?: boolean,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/health-connect/export-range", { startDate, endDate, mode, redacted });
}

export async function openSettingsPanel(ctx: ToolContext, sheet: string | null, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/settings/open", { sheet });
}

export async function goalsState(ctx: ToolContext, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).get("/goals/state");
}

export async function setGoalsSettingsVisible(ctx: ToolContext, visible: boolean, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/goals/settings", { visible });
}

export async function setGoalsProfile(
  ctx: ToolContext,
  input: Record<string, unknown>,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/goals/set-profile", input);
}

export async function setGoalsMeasurement(
  ctx: ToolContext,
  field: string,
  value: number | null,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/goals/set-measurement", { field, value });
}

export async function toggleGoalsMeasurementLock(ctx: ToolContext, field: string, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/goals/toggle-measurement-lock", { field });
}

export async function setGoalsMacro(
  ctx: ToolContext,
  macro: string,
  value: number | null,
  hostPort = DEFAULT_HOST_PORT,
) {
  return ctx.bridgeFor(hostPort).post("/goals/set-macro", { macro, value });
}

export async function toggleGoalsMacroLock(ctx: ToolContext, macro: string, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/goals/toggle-macro-lock", { macro });
}

export async function refreshGoalsHealthConnect(ctx: ToolContext, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/goals/refresh-health-connect", {});
}

export async function recalculateGoals(ctx: ToolContext, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/goals/recalculate", {});
}

export async function applyGoalsRecommendation(ctx: ToolContext, hostPort = DEFAULT_HOST_PORT) {
  return ctx.bridgeFor(hostPort).post("/goals/apply-recommendation", {});
}

export async function screenshot(ctx: ToolContext, deviceSerial: string, outputPath?: string) {
  const path = outputPath ?? join(await mkdtemp(join(tmpdir(), "mcc-shot-")), "screen.png");
  return { path: await ctx.adb.screenshot(deviceSerial, path) };
}

export async function logcat(
  ctx: ToolContext,
  deviceSerial: string,
  packageName = DEFAULT_APP_PACKAGE,
  lines = 300,
) {
  return { logcat: await ctx.adb.logcat(deviceSerial, packageName, lines) };
}

export async function runSmoke(ctx: ToolContext, script: "fold" | "fold_health" | "automation") {
  const scriptPath = script === "fold"
    ? "scripts/android/run-fold-smoke.sh"
    : script === "fold_health"
      ? "scripts/android/run-fold-health-smoke.sh"
      : "scripts/android/run-automation-smoke.sh";
  const { stdout, stderr } = await ctx.run("bash", [scriptPath], { cwd: REPO_ROOT, timeoutMs: 180000 });
  return { stdout, stderr };
}
