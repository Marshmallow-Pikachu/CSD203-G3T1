import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { handleLogin } from "../api/user";
import SocialAuthButtons from "../components/SocialAuthButtons";
import Input from "../components/Input";


export default function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const onSubmit = (e: React.FormEvent) => handleLogin(e, username, password, navigate);

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100">
    <form
      onSubmit={onSubmit}
      className="w-full max-w-sm bg-white shadow-lg rounded-lg p-8 space-y-6"
    >
        <div className="text-center mb-4">
          <h2 className="text-2xl font-bold text-gray-800">Welcome Back</h2>
          <p className="text-sm text-gray-500 mt-1">Sign in to continue to RateWise</p>
        </div>

        {/* <div className="space-y-4"> */}
          <Input
            id="username"
            label="Username"
            value={username}
            onChange={setUsername}
            placeholder="Enter your username"
          />
          <Input
            id="password"
            label="Password"
            type="password"
            value={password}
            onChange={setPassword}
            placeholder="••••••••"
          />
        {/* </div> */}

        <button
          type="submit"
          className="w-full py-2.5 mt-2 bg-blue-600 text-white font-medium rounded-lg 
                    shadow-sm hover:bg-blue-700 hover:shadow-md 
                    focus:outline-none focus:ring-2 focus:ring-blue-400 
                    transition duration-150 ease-in-out"
        >
          Login
        </button>

        <div className="pt-4">
          <SocialAuthButtons />
        </div>

        <p className="text-sm text-center text-gray-600 pt-2">
          Don’t have an account?{" "}
          <Link to="/signup" className="text-blue-600 hover:underline">
            Create one
          </Link>
        </p>
      </form>
    </div>
  );
}