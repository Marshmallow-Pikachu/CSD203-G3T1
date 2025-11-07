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