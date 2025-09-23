import { useState } from "react";
import { api } from "../api/client"; // ✅ import your axios client
import { useNavigate } from "react-router-dom"; // 🔹 NEW

export default function Login() {
  const [email, setEmail] = useState("");       // 🔹 NEW
  const [password, setPassword] = useState(""); // 🔹 NEW
  const [message, setMessage] = useState("");   // 🔹 NEW
  const navigate = useNavigate(); // 🔹 NEW

  async function handleLogin(e: React.FormEvent) { // 🔹 NEW
    e.preventDefault();
    try {
    //   const res = await api.post(
    //     "/auth/login",
    //     { username: email, password }, // backend expects username/password
    //     { withCredentials: true }
    //   );
    // Deal with sessions somehow
      navigate("/");

    //   console.log("Response:", res.data);

    } catch (err) {
      console.error("Login failed:", err);
      setMessage("❌ Login failed");
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100">
      <div className="w-full max-w-md bg-white rounded-lg shadow-lg p-8">
        <h2 className="text-2xl font-bold text-center text-gray-800 mb-6">
          Login to Your Account
        </h2>

        {/* 🔹 CHANGED: added onSubmit */}
        <form className="space-y-6" onSubmit={handleLogin}>
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700">
              Email
            </label>
            <input
              type="email"
              id="email"
              placeholder="you@example.com"
              value={email}                         
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 block w-full px-4 py-2 border rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:outline-none border-gray-300"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700">
              Password
            </label>
            <input
              type="password"
              id="password"
              placeholder="********"
              value={password}                           
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 block w-full px-4 py-2 border rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:outline-none border-gray-300"
            />
          </div>
          <button
            type="submit"
            className="w-full bg-blue-600 text-white font-semibold py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors"
          >
            Login
          </button>
        </form>

        {/* 🔹 NEW: message display */}
        {message && (
          <p className="mt-4 text-center text-sm text-gray-700">{message}</p>
        )}

        <p className="mt-6 text-center text-sm text-gray-600">
          Don’t have an account?{" "}
          <a href="/register" className="text-blue-600 hover:underline">
            Sign up
          </a>
        </p>
      </div>
    </div>
  );
}