import { execFile } from "node:child_process";
import { writeFile } from "node:fs/promises";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

export type CommandRunner = (
  command: string,
  args: string[],
  options?: { cwd?: string; timeoutMs?: number }
) => Promise<{ stdout: string; stderr: string }>;

export const defaultRunner: CommandRunner = async (command, args, options = {}) => {
  const result = await execFileAsync(command, args, {
    cwd: options.cwd,
    timeout: options.timeoutMs ?? 30000,
    maxBuffer: 10 * 1024 * 1024,
  });
  return {
    stdout: result.stdout,
    stderr: result.stderr,
  };
};

export interface AndroidDevice {
  serial: string;
  state: string;
  isEmulator: boolean;
}

export class AdbClient {
  constructor(private readonly run: CommandRunner = defaultRunner) {}

  async listDevices(): Promise<AndroidDevice[]> {
    const { stdout } = await this.run("adb", ["devices"]);
    return stdout
      .split(/\r?\n/)
      .slice(1)
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const [serial, state = "unknown"] = line.split(/\s+/);
        return {
          serial,
          state,
          isEmulator: serial.startsWith("emulator-"),
        };
      });
  }

  async forward(serial: string, hostPort: number, devicePort: number): Promise<void> {
    await this.run("adb", ["-s", serial, "forward", `tcp:${hostPort}`, `tcp:${devicePort}`]);
  }

  async screenshot(serial: string, outputPath: string): Promise<string> {
    const result = await execFileAsync("adb", ["-s", serial, "exec-out", "screencap", "-p"], {
      timeout: 30000,
      maxBuffer: 10 * 1024 * 1024,
      encoding: "buffer",
    });
    await writeFile(outputPath, result.stdout);
    return outputPath;
  }

  async logcat(serial: string, packageName: string, lines: number): Promise<string> {
    const pid = await this.run("adb", ["-s", serial, "shell", "pidof", "-s", packageName])
      .then((result) => result.stdout.trim())
      .catch(() => "");
    const args = pid
      ? ["-s", serial, "logcat", "--pid", pid, "-d", "-t", String(lines)]
      : ["-s", serial, "logcat", "-d", "-t", String(lines)];
    const { stdout } = await this.run("adb", args, { timeoutMs: 30000 });
    return stdout;
  }
}
