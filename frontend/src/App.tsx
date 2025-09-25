import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Home from './pages/Home';
import Login from './pages/Login';
import Calculator from './pages/Calculator';
import Navbar from './components/Navbar';


function PrivateRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem("accessToken");
  return token ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
      {/* Public route */}
      <Route path="/login" element={<Login />} />

      {/* Protected routes */}
        <Route path="/" element={<PrivateRoute><Navbar /><Home /></PrivateRoute>} />
        <Route path="/calculator" element={<PrivateRoute><Navbar /><Calculator /></PrivateRoute>} />
      </Routes>
    </BrowserRouter>
    
  );
}
