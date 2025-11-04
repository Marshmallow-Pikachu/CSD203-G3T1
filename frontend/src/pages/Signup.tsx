import { useState } from "react";
import { api } from "../api/client";
import { useNavigate, Link } from "react-router-dom";
import Button from "../components/Button";
import SocialAuthButtons from "../components/SocialAuthButtons";
import toast from "react-hot-toast";

export default function Signup() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState(""); 
  const [confirm, setConfirm]   = useState("");

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== confirm) {
      alert("Passwords do not match");
      return;
    }

    try {
      // TODO: point to your real registration endpoint
      const res = await api.post(
        "/api/v1/auth/registration",
        { username, email, password },
        { headers: { "Content-Type": "application/json" } }
      );

      // You can auto-login after signup or redirect to /login
      // Here, we’ll redirect to /login by default
      if (res.status === 201 || res.status === 200) {
        toast.success("Signup successful!");
        setTimeout(() => navigate("/login"), 1500);

      }
    } catch (error: any) {
      console.error("Signup failed", error.response?.data || error.message);
      alert("Signup failed");
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <form
        onSubmit={handleSignup}
        className="w-full max-w-sm bg-white shadow-md rounded-lg p-6 space-y-6"
      >
        <h2 className="text-2xl font-bold text-center text-gray-800">
          Create your account
        </h2>

        <div className="space-y-1">
          <label htmlFor="username" className="block text-sm font-medium text-gray-600">
            Username
          </label>
          <input
            id="username" type="text" value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Choose a username"
            className="block w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
            required
          />
        </div>

        <div className="space-y-1">
          <label htmlFor="email" className="block text-sm font-medium text-gray-600">
            Email
          </label>
          <input
            id="email" type="email" value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            className="block w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
            required
          />
        </div>

        <div className="space-y-1">
          <label htmlFor="password" className="block text-sm font-medium text-gray-600">
            Password
          </label>
          <input
            id="password" type="password" value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            className="block w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
            required
          />
        </div>

        <div className="space-y-1">
          <label htmlFor="confirm" className="block text-sm font-medium text-gray-600">
            Confirm password
          </label>
          <input
            id="confirm" type="password" value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            placeholder="••••••••"
            className="block w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
            required
          />
        </div>

        <div className="flex justify-center">
          <Button type="submit">Create account</Button>
        </div>

        <p className="text-sm text-center text-gray-600">
          Already have an account?{" "}
          <Link to="/login" className="text-blue-600 hover:underline">
            Log in
          </Link>
        </p>
      </form>
    </div>
  );
}