import { User, LogOut } from "lucide-react";
import { handleLogout } from "../../../api/user";

export function SidebarFooter({
  collapsed,
  navigate,
}: {
  collapsed: boolean;
  navigate: (path: string) => void;
}) {
  const token = localStorage.getItem("accessToken");
  const isAuthenticated = !!token;

  return (
    <div className="border-t border-gray-200 p-2">
      {isAuthenticated && (
        <>
          <button
            onClick={() => navigate("/profile")}
            className={`mt-2 flex w-full items-center rounded-xl text-sm font-medium transition-colors ${
              collapsed ? "justify-center p-2" : "gap-3 px-3 py-2"
            } border border-gray-300 hover:bg-gray-100`}
          >
            <User className="h-5 w-5" />
            {!collapsed && <span>Profile</span>}
          </button>

          <button
            onClick={() => handleLogout(token, navigate)}
            className={`mt-2 flex w-full items-center rounded-xl text-sm font-medium transition-colors text-red-700 ${
              collapsed ? "justify-center p-2" : "gap-3 px-3 py-2"
            } border border-red-300 hover:bg-red-50`}
          >
            <LogOut className="h-5 w-5" />
            {!collapsed && <span>Logout</span>}
          </button>
        </>
      )}
    </div>
  );
}