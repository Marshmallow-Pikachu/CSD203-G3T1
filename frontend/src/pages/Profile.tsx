import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import Button from "../components/buttons/Button";
import { handleLogout, getProfile} from "../api/user";

type UserProfile = { username: string, role: string };

export default function Profile() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  // Read once; no need to keep token in React state
  const token = localStorage.getItem("accessToken");

useEffect(() => {
  // If no token, bounce to login
  if (!token) {
    toast.error("No token found. Please log in again.");
    navigate("/login");
    return;
  }


  let cancelled = false;
  const fetchProfile = async () => {
    try {
      const data = await getProfile(token); 
      if (!cancelled) setProfile(data);
    } catch (err) {
      if (!cancelled) {
        console.error("Failed to load profile", err);
        toast.error("Session expired. Please log in again.");
        localStorage.removeItem("accessToken");
        navigate("/login");
      }
    } finally {
      if (!cancelled) setLoading(false);
    }
  };

  fetchProfile();

  return () => {
    cancelled = true; // avoid state updates after unmount
  };
}, [navigate, token]);


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
          <div>
          <p className="text-gray-700">
            <strong>Username:</strong> {profile.username}
          </p>
          <p className="text-gray-700">
            <strong>Role:</strong> {profile.role}
          </p>
          </div>
        ) : (
          <p className="text-gray-500">No profile data available.</p>
        )}
        <div className="pt-4">
          <Button onClick={() => handleLogout(token, navigate)}>Logout</Button>
        </div>
      </div>
    </div>
  );
}