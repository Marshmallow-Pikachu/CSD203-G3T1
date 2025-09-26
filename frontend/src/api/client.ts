// src/api/client.ts
import axios from "axios";
import type {
  AxiosError,
  AxiosHeaders,
  InternalAxiosRequestConfig,
  AxiosRequestConfig,
} from "axios";

/* ===========================
   Token utilities
   =========================== */
const ACCESS_TOKEN_KEY = "accessToken";

const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY);
export const setAccessToken = (t: string) => localStorage.setItem(ACCESS_TOKEN_KEY, t);
export const clearAccessToken = () => localStorage.removeItem(ACCESS_TOKEN_KEY);

// Optional: parse JWT payload (unused but handy if you later pre-empt refresh)
export const parseJwt = (token?: string) => {
  if (!token) return null;
  const [, payload] = token.split(".");
  try {
    return JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
  } catch {
    return null;
  }
};

/* ===========================
   Base URL (env-first)
   =========================== */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

/* ===========================
   Axios instance
   =========================== */
export const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // send cookies if your backend uses them (e.g., refresh token)
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

/* ===========================
   Request interceptor
   - attach Bearer token safely
   - skip /auth endpoints
   - redact token in logs
   =========================== */
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    // 1) Read token (handle accidental JSON-quoted storage)
    let jwtToken = getAccessToken() || undefined;
    if (jwtToken && /^".*"$/.test(jwtToken)) {
      try {
        jwtToken = JSON.parse(jwtToken);
      } catch {
        /* ignore */
      }
    }

    // 2) Skip attaching Authorization to login/refresh endpoints
    const url = config.url || "";
    const isAuthEndpoint =
      url.startsWith("/auth/login") || url.startsWith("/auth/refresh");

    // 3) Set Authorization header safely for Axios v1 (AxiosHeaders supports .set())
    if (jwtToken && !isAuthEndpoint) {
      (config.headers as AxiosHeaders).set("Authorization", `Bearer ${jwtToken}`);
    }

    // 4) Safe logging (redact token; handle absolute vs relative URLs)
    const headersForLog =
      config.headers && typeof (config.headers as any).toJSON === "function"
        ? (config.headers as AxiosHeaders).toJSON()
        : { ...(config.headers as Record<string, unknown>) };

    if (headersForLog && (headersForLog as any).Authorization) {
      (headersForLog as any).Authorization = "Bearer ***";
    }

    const isAbsolute = /^https?:\/\//i.test(url);
    const fullUrl = isAbsolute ? url : `${config.baseURL ?? ""}${url}`;

    console.log("Sending request:", {
      method: config.method?.toUpperCase(),
      url: fullUrl,
      headers: headersForLog,
      hasBody: !!config.data, // avoid dumping full body (may contain secrets)
    });

    return config;
  },
  (error: unknown) => {
    console.error("Request setup error:", error);
    return Promise.reject(error);
  }
);

/* ===========================
   Response interceptor
   - normalize error messages
   (clean Error(message) for UI)
   =========================== */
api.interceptors.response.use(
  (res) => res,
  (err: AxiosError) => {
    const cfg = err.config as (AxiosRequestConfig & { url?: string }) | undefined;
    const status = err.response?.status ?? null;

    // Default normalized error
    const normalizedError = {
      ok: false,
      status,
      message: "Unexpected error occurred.",
      hint: null as string | null,
      raw: err.response?.data || null,
    };

    if (err.response?.data) {
      const data = err.response.data as any;
      normalizedError.message =
        data.message || data.error || normalizedError.message;
      if (data.hint) normalizedError.hint = data.hint;
    } else if (err.message) {
      normalizedError.message = err.message;
    }

    console.error("API error:", {
      url: cfg?.url,
      status: normalizedError.status,
      message: normalizedError.message,
      hint: normalizedError.hint,
    });

    return Promise.reject(normalizedError);
  }
);

