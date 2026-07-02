export interface BridgeOptions {
  host?: string;
  port?: number;
}

export class MccBridgeClient {
  private readonly baseUrl: string;

  constructor(options: BridgeOptions = {}) {
    const host = options.host ?? "127.0.0.1";
    const port = options.port ?? 18765;
    this.baseUrl = `http://${host}:${port}`;
  }

  get(path: string): Promise<unknown> {
    return this.request("GET", path);
  }

  post(path: string, body: Record<string, unknown>): Promise<unknown> {
    return this.request("POST", path, body);
  }

  private async request(method: "GET" | "POST", path: string, body?: Record<string, unknown>): Promise<unknown> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers: body ? { "content-type": "application/json" } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
    const text = await response.text();
    let parsed: unknown;
    try {
      parsed = text ? JSON.parse(text) : null;
    } catch {
      throw new Error(`Bridge returned non-JSON response (${response.status}): ${text.slice(0, 300)}`);
    }
    if (!response.ok) {
      const message = typeof parsed === "object" && parsed && "error" in parsed
        ? String((parsed as { error: unknown }).error)
        : response.statusText;
      throw new Error(`Bridge request failed: ${message}`);
    }
    return parsed;
  }
}
