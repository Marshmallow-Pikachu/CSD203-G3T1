// src/pages/OAuthCallback.tsx
import { useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";

/**
 * Handles OAuth2 callback redirect from the backend.
 * - Reads the JWT token from query params (?token=...) or hash (#token=...).
 * - Saves it in localStorage.
 * - Redirects the user to /home if successful, or /login if missing/invalid.
 */
export default function OAuthCallback() {
  const navigate = useNavigate();
  const { search, hash } = useLocation();

  useEffect(() => {
    let token: string | null = null;

    // Try to read from query params (?token=...)
    const queryParams = new URLSearchParams(search);
    token = queryParams.get("token");

    // Fallback: try to read from hash fragment (#token=...)
    if (!token && hash) {
      const hashParams = new URLSearchParams(hash.startsWith("#") ? hash.slice(1) : hash);
      token = hashParams.get("token");
    }

    // Handle redirect logic
    if (token) {
      try {
        localStorage.setItem("accessToken", token);
        console.log("OAuth token stored successfully.");
        navigate("/home", { replace: true });
      } catch (err) {
        console.error("Failed to save token:", err);
        navigate("/login", { replace: true });
      }
    } else {
      console.warn("No token found in URL.");
      navigate("/login", { replace: true });
    }
  }, [search, hash, navigate]);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 text-gray-700">
      <div className="text-lg font-medium mb-2">Signing you in...</div>
      <div className="text-sm opacity-75">Please wait a moment while we complete your login.</div>
    </div>
  );
}