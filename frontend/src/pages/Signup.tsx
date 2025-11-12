import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { signupUser } from "../api/user";
import Button from "../components/buttons/Button";
import Input from "../components/forms/Input";


const isValidEmail = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
const isStrongPassword = (v: string) => /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(v);

export default function Signup() {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();

    const u = username.trim();
    const em = email.trim();

    if (!isValidEmail(em)) return toast.error("Please enter a valid email address.");
    if (!isStrongPassword(password))
      return toast.error("Password must include 1 uppercase, 1 lowercase, 1 number, 8+ chars.");
    if (password !== confirm) return toast.error("Passwords do not match.");

    try {
      await signupUser({ username: u, email: em, password });
      toast.success("Signup successful!");
      setTimeout(() => navigate("/login"), 1200);
    } catch (error: any) {
      console.error("Signup failed", error?.response?.data || error?.message);
      toast.error("Signup failed. Please try again.");
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <form
        onSubmit={handleSignup}
        className="w-full max-w-sm bg-white shadow-md rounded-lg p-6 space-y-6"
      >
        <h2 className="text-2xl font-bold text-center text-gray-800">Create your account</h2>

        <Input id="username" label="Username" value={username} onChange={setUsername} placeholder="Choose a username" />

        <div>
          <Input id="email" label="Email" type="email" value={email} onChange={setEmail} placeholder="you@example.com" />
          <p className="text-xs text-gray-500 mt-1">
            Please enter a valid email (e.g. user@example.com).
          </p>
        </div>

        <div>
          <Input id="password" label="Password" type="password" value={password} onChange={setPassword} placeholder="••••••••" />
          <p className="text-xs text-gray-500 mt-1">
            Must include at least 1 uppercase, 1 lowercase, 1 number, and be 8+ characters.
          </p>
        </div>

        <Input id="confirm" label="Confirm password" type="password" value={confirm} onChange={setConfirm} placeholder="••••••••" />

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