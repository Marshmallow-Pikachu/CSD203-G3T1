// src/pages/OAuthCallback.tsx
import { useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { handleOAuthCallback } from "../api/user";

/**
 * OAuthCallback component
 * Handles OAuth2 callback redirect from the backend.
 * Delegates token extraction and redirect logic to handleOAuthCallback() in user.ts.
 */
export default function OAuthCallback() {
  const navigate = useNavigate();
  const { search, hash } = useLocation();

  useEffect(() => {
    // Delegate all logic to helper in user.ts
    handleOAuthCallback(search, hash, navigate);
  }, [search, hash, navigate]);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 text-gray-700">
      <div className="text-lg font-medium mb-2">Signing you in...</div>
      <div className="text-sm opacity-75">
        Please wait a moment while we complete your login.
      </div>
    </div>
  );
}