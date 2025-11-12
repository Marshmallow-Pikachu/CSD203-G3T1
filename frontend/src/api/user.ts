// src/api/user.ts
import { api } from "./client";
import { toast } from "react-hot-toast";
import type { NavigateFunction } from "react-router-dom";


export const handleLogin = async (
  e: React.FormEvent,
  username: string,
  password: string,
  navigate: (path: string) => void
) => {
  e.preventDefault();

  // Debug: show full HTTP request details
  const requestDetails = {
    method: "POST",
    url: "/api/v1/auth/session",
    headers: { "Content-Type": "application/json" },
    body: { username, password },
  };

  // alert("Sending HTTP Request:\n\n" + JSON.stringify(requestDetails, null, 2));

  try {
    const res = await api.post(
      requestDetails.url,
      requestDetails.body,
      { headers: requestDetails.headers }
    );

    const token = res.data.accessToken;
    localStorage.setItem("accessToken", token);
    localStorage.setItem("username", username);
    localStorage.setItem("userRole", res.data.role);
    // console.log("Login successful, received token:", token);

    toast.success("Login successful!");
    setTimeout(() => navigate("/home"), 1500);
  } catch (error: any) {
    // const msg = error?.response?.data?.message || error.message || "Unknown error";

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


export type SignupPayload = {
  username: string;
  email: string;
  password: string;
};

export async function signupUser(payload: SignupPayload) {
  const res = await api.post("/api/v1/auth/registration", payload, {
    headers: { "Content-Type": "application/json" },
  });
  return res.data;
}


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


export type UserProfile = { username: string; role: string };

export async function getProfile(token: string): Promise<UserProfile> {
  const { data } = await api.get<UserProfile>("/api/v1/auth/profile", {
    headers: { Authorization: `Bearer ${token}` },
  });
  return data;
}


export function handleOAuthCallback(
  search: string,
  hash: string,
  navigate: NavigateFunction
) {
  let token: string | null = null;

  //Try reading from query params (?token=...)
  const queryParams = new URLSearchParams(search);
  token = queryParams.get("token");

  // Fallback: read from hash (#token=...)
  if (!token && hash) {
    const hashParams = new URLSearchParams(hash.startsWith("#") ? hash.slice(1) : hash);
    token = hashParams.get("token");
  }

  //Store token and navigate
  if (token) {
    try {
      localStorage.setItem("accessToken", token);
      console.log("OAuth token stored successfully.");
      getProfile(token).then(profile => {
        localStorage.setItem("username", profile.username);
        localStorage.setItem("userRole", profile.role);
      }).catch(err => {
        console.error("Failed to fetch profile after OAuth login:", err);
      });
      navigate("/home", { replace: true });
    } catch (err) {
      console.error("Failed to save token:", err);
      navigate("/login", { replace: true });
    }
  } else {
    console.warn("No token found in URL.");
    navigate("/login", { replace: true });
  }
}
