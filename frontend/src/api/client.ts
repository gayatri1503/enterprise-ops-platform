import type { LoginResponse, UserSummary } from "../types";

// The access token lives only in memory (not localStorage), so an XSS bug
// cannot steal it. The refresh token is an HttpOnly cookie the JS never sees.
let accessToken: string | null = null;
let refreshInFlight: Promise<UserSummary | null> | null = null;
let onSessionExpired: (() => void) | null = null;

export class ApiRequestError extends Error {
  status: number;
  code: string;
  details: string[];

  constructor(status: number, code: string, message: string, details: string[] = []) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

export function setSessionExpiredHandler(fn: () => void) {
  onSessionExpired = fn;
}

async function parseError(res: Response): Promise<ApiRequestError> {
  try {
    const body = await res.json();
    const details: string[] = Array.isArray(body.validationErrors)
      ? body.validationErrors.map((e: { field: string; message: string }) => `${e.field}: ${e.message}`)
      : [];
    return new ApiRequestError(res.status, body.code ?? "ERROR", body.message ?? "Request failed", details);
  } catch {
    return new ApiRequestError(res.status, "ERROR", `Request failed (${res.status})`);
  }
}

export async function login(email: string, password: string): Promise<UserSummary> {
  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "same-origin",
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) throw await parseError(res);
  const data: LoginResponse = await res.json();
  accessToken = data.accessToken;
  return data.user;
}

export async function register(fullName: string, email: string, password: string): Promise<UserSummary> {
  const res = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "same-origin",
    body: JSON.stringify({ fullName, email, password }),
  });
  if (!res.ok) throw await parseError(res);
  const data: LoginResponse = await res.json();
  accessToken = data.accessToken;
  return data.user;
}
// Single-flight: if several calls need a refresh at once (or React StrictMode
// runs an effect twice), they share ONE request. Refresh tokens rotate, so two
// parallel refreshes would look like token reuse to the server.
export function refreshSession(): Promise<UserSummary | null> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const res = await fetch("/api/auth/refresh", { method: "POST", credentials: "same-origin" });
      if (!res.ok) {
        accessToken = null;
        return null;
      }
      const data: LoginResponse = await res.json();
      accessToken = data.accessToken;
      return data.user;
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

export async function logout(): Promise<void> {
  try {
    await fetch("/api/auth/logout", { method: "POST", credentials: "same-origin" });
  } finally {
    accessToken = null;
  }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const send = () => {
    const headers = new Headers(init.headers);
    if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
    return fetch(path, { ...init, headers, credentials: "same-origin" });
  };

  let res = await send();

  // Expired access token -> refresh exactly once, then retry the original call.
  if (res.status === 401) {
    const user = await refreshSession();
    if (!user) {
      onSessionExpired?.();
      throw new ApiRequestError(401, "UNAUTHORIZED", "Session expired. Please sign in again.");
    }
    res = await send();
  }

  if (!res.ok) throw await parseError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}