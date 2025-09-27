import { Outlet } from "react-router-dom";
import Sidebar from "../CollapsibleSidebar";

export default function AppLayout() {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <main className="flex-1 p-4 lg:p-6">
        <Outlet />
      </main>
    </div>
  );
}
