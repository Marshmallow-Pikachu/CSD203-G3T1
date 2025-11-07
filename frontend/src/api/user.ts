// src/api/logout.ts
import { api } from "./client";
import { toast } from "react-hot-toast";
import type { NavigateFunction } from "react-router-dom";

export const handleLogout = async (
  token: string | null,
  navigate?: NavigateFunction
): Promise<void> => {
  try {
    if (token) {
      await api.delete("/api/v1/auth/session", {
        headers: { Authorization: `Bearer ${token}` },
      });
    }
  } catch (e) {
    console.warn("Logout request failed, clearing token anyway", e);
  } finally {
    localStorage.removeItem("accessToken");
    toast.success("Logged out successfully");

    if (typeof navigate === "function") {
      // safer navigation
      navigate("/login", { replace: true });
    } else {
      // fallback if called outside of router context
      window.location.replace("/login");
    }
  }
};

export const handleLogin = async (
  e: React.FormEvent,
  username: string,
  password: string,
  navigate: (path: string) => void
) => {
  e.preventDefault();

  // 👀 Debug: show full HTTP request details
  const requestDetails = {
    method: "POST",
    url: "/api/v1/auth/session",
    headers: { "Content-Type": "application/json" },
    body: { username, password },
  };

  // alert("📤 Sending HTTP Request:\n\n" + JSON.stringify(requestDetails, null, 2));

  try {
    const res = await api.post(
      requestDetails.url,
      requestDetails.body,
      { headers: requestDetails.headers }
    );

    const token = res.data.accessToken;
    localStorage.setItem("accessToken", token);
    localStorage.setItem("username", username);

    toast.success("Login successful!");
    setTimeout(() => navigate("/home"), 1500);
  } catch (error: any) {
    const msg = error?.response?.data?.message || error.message || "Unknown error";

    // // Debug failed request
    // alert(
    //   "Login failed!\n\n" +
    //   "Request:\n" + JSON.stringify(requestDetails, null, 2) +
    //   "\n\nResponse Error:\n" + msg
    // );

    console.error("Login failed:", error.response?.data || error.message);
    toast.error("Login failed. Please check your credentials.");
  }
};
