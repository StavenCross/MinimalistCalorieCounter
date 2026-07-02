import test from "node:test";
import assert from "node:assert/strict";
import { AdbClient, CommandRunner } from "../adb.js";
import {
  ConnectInput,
  createToolContext,
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
