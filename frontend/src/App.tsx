// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import AppLayout from "./components/layouts/AppLayout";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Calculator from "./pages/Calculator";
import Tariffs from "./pages/Tariffs"; 
import Signup from "./pages/Signup";

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem("accessToken");
  return token ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Login as the entry point */}
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
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
        </Route>

        {/* Catch-all → redirect to login */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
