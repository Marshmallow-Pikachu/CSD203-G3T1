import React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { SidebarHeader } from "./SidebarHeader";
import { SidebarNav } from "./SidebarNav";
import { SidebarFooter } from "./SidebarFooter";

export function classNames(...parts: Array<string | false | null | undefined>) {
  return parts.filter(Boolean).join(" ");
}
export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();

  const [collapsed, setCollapsed] = React.useState(false);
  const [mobileOpen, setMobileOpen] = React.useState(false);

  // Hide on login
  if (location.pathname.startsWith("/login")) return null;

  // Keep collapse state
  React.useEffect(() => {
    const saved = localStorage.getItem("sidebar:collapsed");
    if (saved) setCollapsed(saved === "1");
  }, []);
  React.useEffect(() => {
    localStorage.setItem("sidebar:collapsed", collapsed ? "1" : "0");
  }, [collapsed]);

  return (
    <>
      {/* Mobile Header */}
      <div className="sticky top-0 z-40 flex h-14 items-center gap-2 border-b border-gray-200 bg-white px-3 backdrop-blur md:hidden">
        <button
          onClick={() => setMobileOpen(true)}
          className="rounded-xl p-2 hover:bg-gray-100"
          aria-label="Open sidebar"
        >
          ☰
        </button>
        <span className="text-base font-semibold tracking-tight">RateWise</span>
      </div>

      {/* Sidebar Body */}
      <aside className={classNames("hidden md:block")}>
        <div className="fixed inset-y-0 left-0 z-40">
          <div
            className={classNames(
              "flex h-full flex-col border-r border-gray-200 bg-white transition-all duration-200",
              collapsed ? "w-20" : "w-72"
            )}
          >
            <SidebarHeader collapsed={collapsed} setCollapsed={setCollapsed} />
            <SidebarNav collapsed={collapsed} />
            <SidebarFooter collapsed={collapsed} navigate={navigate} />
          </div>
        </div>
        {/* Spacer */}
        <div className={classNames("transition-all duration-200", collapsed ? "w-20" : "w-72")} />
      </aside>

      {/* Mobile Drawer */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-50 md:hidden"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/40"
            onClick={() => setMobileOpen(false)}
          />
          <div className="absolute inset-y-0 left-0">
            <div className="flex h-full flex-col border-r border-gray-200 bg-white w-64">
              <SidebarHeader collapsed={false} setCollapsed={setCollapsed} />
              <SidebarNav collapsed={false} />
              <SidebarFooter collapsed={false} navigate={navigate} />
            </div>
          </div>
        </div>
      )}
    </>
  );
}