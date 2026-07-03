import test from "node:test";
import assert from "node:assert/strict";
import { AdbClient, CommandRunner } from "../adb.js";
import {
  ConnectInput,
  createToolContext,
  exportHealthRange,
  openSettingsPanel,
  quickImportRetry,
  setGoalsMacro,
  setGoalsProfile,
  toggleGoalsMeasurementLock,
  listDevices,
  navigate,
} from "../tools.js";

test("lists adb devices and marks emulators", async () => {
  const run: CommandRunner = async () => ({
    stdout: "List of devices attached\nemulator-5554\tdevice\nABC123\tdevice\n",
    stderr: "",
  });
  const adb = new AdbClient(run);

  assert.deepEqual(await adb.listDevices(), [
    { serial: "emulator-5554", state: "device", isEmulator: true },
    { serial: "ABC123", state: "device", isEmulator: false },
  ]);
});

test("connect input defaults to the expected port pair", () => {
  assert.deepEqual(ConnectInput.parse({ deviceSerial: "emulator-5554" }), {
    deviceSerial: "emulator-5554",
    hostPort: 18765,
    devicePort: 8765,
  });
});

test("navigate posts to the bridge", async () => {
  const calls: Array<{ path: string; body: Record<string, unknown> }> = [];
  const ctx = {
    ...createToolContext(async () => ({ stdout: "", stderr: "" })),
    bridgeFor: () => ({
      post: async (path: string, body: Record<string, unknown>) => {
        calls.push({ path, body });
        return { ok: true };
      },
      get: async () => ({ ok: true }),
    }),
  };

  assert.deepEqual(await navigate(ctx, "quick_add"), { ok: true });
  assert.deepEqual(calls, [{ path: "/navigate", body: { screen: "quick_add" } }]);
});

test("listDevices uses injected runner", async () => {
  const ctx = createToolContext(async () => ({
    stdout: "List of devices attached\nemulator-5554\tdevice\n",
    stderr: "",
  }));

  assert.deepEqual(await listDevices(ctx), [
    { serial: "emulator-5554", state: "device", isEmulator: true },
  ]);
});

test("goals tools post to the bridge", async () => {
  const calls: Array<{ path: string; body: Record<string, unknown> }> = [];
  const ctx = {
    ...createToolContext(async () => ({ stdout: "", stderr: "" })),
    bridgeFor: () => ({
      post: async (path: string, body: Record<string, unknown>) => {
        calls.push({ path, body });
        return { ok: true };
      },
      get: async () => ({ ok: true }),
    }),
  };

  await setGoalsProfile(ctx, { sex: "Male", heightCm: 180 }, 18765);
  await setGoalsMacro(ctx, "Calories", 2100, 18765);
  await toggleGoalsMeasurementLock(ctx, "WeightKg", 18765);

  assert.deepEqual(calls, [
    { path: "/goals/set-profile", body: { sex: "Male", heightCm: 180 } },
    { path: "/goals/set-macro", body: { macro: "Calories", value: 2100 } },
    { path: "/goals/toggle-measurement-lock", body: { field: "WeightKg" } },
  ]);
});

test("health export posts date range to the bridge", async () => {
  const calls: Array<{ path: string; body: Record<string, unknown> }> = [];
  const ctx = {
    ...createToolContext(async () => ({ stdout: "", stderr: "" })),
    bridgeFor: () => ({
      post: async (path: string, body: Record<string, unknown>) => {
        calls.push({ path, body });
        return { ok: true };
      },
      get: async () => ({ ok: true }),
    }),
  };

  assert.deepEqual(await exportHealthRange(ctx, "2026-07-01", "2026-07-02", 18765), { ok: true });
  assert.deepEqual(calls, [
    {
      path: "/health-connect/export-range",
      body: { startDate: "2026-07-01", endDate: "2026-07-02" },
    },
  ]);
});

test("settings panel tool posts sheet key to the bridge", async () => {
  const calls: Array<{ path: string; body: Record<string, unknown> }> = [];
  const ctx = {
    ...createToolContext(async () => ({ stdout: "", stderr: "" })),
    bridgeFor: () => ({
      post: async (path: string, body: Record<string, unknown>) => {
        calls.push({ path, body });
        return { ok: true };
      },
      get: async () => ({ ok: true }),
    }),
  };

  await openSettingsPanel(ctx, "theme", 18765);
  await openSettingsPanel(ctx, "maintenance", 18765);

  assert.deepEqual(calls, [
    { path: "/settings/open", body: { sheet: "theme" } },
    { path: "/settings/open", body: { sheet: "maintenance" } },
  ]);
});

test("quick import retry posts outbox id to the bridge", async () => {
  const calls: Array<{ path: string; body: Record<string, unknown> }> = [];
  const ctx = {
    ...createToolContext(async () => ({ stdout: "", stderr: "" })),
    bridgeFor: () => ({
      post: async (path: string, body: Record<string, unknown>) => {
        calls.push({ path, body });
        return { ok: true };
      },
      get: async () => ({ ok: true }),
    }),
  };

  assert.deepEqual(await quickImportRetry(ctx, "abc123", 18765), { ok: true });
  assert.deepEqual(calls, [{ path: "/quick-import/retry", body: { id: "abc123" } }]);
});
