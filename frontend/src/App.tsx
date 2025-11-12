// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import AppLayout from "./components/layouts/AppLayout";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Calculator from "./pages/Calculator";
import Tariffs from "./pages/Tariffs";
import Signup from "./pages/Signup";
import OAuthCallback from "./pages/oauth-callback";
import Profile from "./pages/Profile";
import GraphPage from "./pages/GraphPage";
import TableAdmin from "./pages/Admin";

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem("accessToken");
  const location = useLocation();

  if (token) return <>{children}</>;
  // preserve where the user was trying to go
  return <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/oauth-callback" element={<OAuthCallback />} />
        <Route path="/" element={<Navigate to="/login" replace />} />

        {/* Protected routes */}
        <Route
          element={
            <PrivateRoute>
              <AppLayout />
            </PrivateRoute>
          }
        >
          <Route path="/home" element={<Home />} />
          <Route path="/calculator" element={<Calculator />} />
          <Route path="/tariffs" element={<Tariffs />} />
          <Route path="/admin" element={<TableAdmin />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/graph" element={<GraphPage />} />
        </Route>

        {/* Catch-all → redirect to login */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>

      {/* Global toast host (mounted once, survives route changes) */}
      <Toaster position="top-center" toastOptions={{ duration: 1000 }} />
    </BrowserRouter>
  );
}