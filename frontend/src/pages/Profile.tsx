import { useEffect, useState } from "react";
import { api } from "../api/client";
import { useNavigate } from "react-router-dom";
import Button from "../components/Button";
import { toast } from "react-hot-toast";

interface UserProfile {
  username: string;
  oauthProvider?: string | null;
}

export default function Profile() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState<string | null>(null);

  // Fetch user profile on mount
  useEffect(() => {
    const jwt = localStorage.getItem("accessToken");
    if (!jwt) {
      toast.error("No token found. Please log in again.");
      navigate("/login");
      return;
    }
    setToken(jwt);

    const fetchProfile = async () => {
      try {
        const res = await api.get("/api/v1/auth/profile", {
          headers: { Authorization: `Bearer ${jwt}` },
        });
        setProfile(res.data);
      } catch (error: any) {
        console.error("Failed to load profile", error);
        toast.error("Session expired. Please log in again.");
        localStorage.removeItem("accessToken");
        navigate("/login");
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [navigate]);

  const handleLogout = async () => {
    try {
      await api.delete("/api/v1/auth/session", {
        headers: { Authorization: `Bearer ${token}` },
      });
    } catch (e) {
      console.warn("Logout request failed, clearing token anyway");
    } finally {
      localStorage.removeItem("accessToken");
      toast.success("Logged out successfully");
      navigate("/login");
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen text-gray-600">
        Loading profile...
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50">
      <div className="bg-white shadow-md rounded-lg p-6 w-full max-w-md text-center space-y-4">
        <h2 className="text-2xl font-bold text-gray-800">User Profile</h2>

        {profile ? (
          <>
            <p className="text-gray-700">
              <strong>Username:</strong> {profile.username}
            </p>
            {/* <p className="text-gray-700">
              <strong>Login Method:</strong>{" "}
              {profile.oauthProvider
                ? profile.oauthProvider.toUpperCase()
                : "Email/Password"}
            </p> */}
          </>
        ) : (
          <p className="text-gray-500">No profile data available</p>
        )}

        {/* Debug section for token */}
        {token && (
          <div className="mt-4 text-left bg-gray-100 rounded-lg p-3">
            <p className="text-xs font-mono text-gray-600 break-all">
              <strong>JWT Token (for testing):</strong>
              <br />
              {token}
            </p>
          </div>
        )}

        <div className="pt-4">
          <Button onClick={handleLogout}>Logout</Button>
        </div>
      </div>
    </div>
  );
}