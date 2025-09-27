import { useState } from "react";
import { api } from "../api/client";
import { useNavigate } from "react-router-dom";
import Button from "../components/Button";

export default function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState(""); // now blank
  const [password, setPassword] = useState(""); // now blank

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const res = await api.post("/api/v1/auth/login", {
        username,
        password,
      });

      const token = res.data.accessToken;
      localStorage.setItem("accessToken", token);

      console.log("JWT Token:", token);
      navigate("/home");
    } catch (error: any) {
      console.error("Login failed", error.response?.data || error.message);
      alert("Login failed");
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <form
        onSubmit={handleLogin}
        className="w-full max-w-sm bg-white shadow-md rounded-lg p-6 space-y-6"
      >
        <h2 className="text-2xl font-bold text-center text-gray-800">
          Sign in to your account
        </h2>

        {/* Username field */}
        <div className="space-y-1">
          <label htmlFor="username" className="block text-sm font-medium text-gray-600">
            Username
          </label>

          {/* To change into Component? */}
          <input type="text" id="username" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Enter your username" className="block w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none" required/>
        </div>

        {/* Password field */}
        <div className="space-y-1">
          <label htmlFor="password" className="block text-sm font-medium text-gray-600"> Password </label>
          <input type="password" id="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" className="block w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none" required/>
        </div>

        <div className="flex justify-center">
          <Button type="submit">Login</Button>
        </div>
      </form>
    </div>
  );
}