import React from "react";
import { NavLink, useLocation } from "react-router-dom";
import {
  PanelLeftClose,
  PanelLeftOpen,
  LayoutGrid,
  Calculator,
  Table,
  User,
  LogOut,
  ChartSpline
} from "lucide-react";

const navItems = [
  { to: "/home", label: "Dashboard", icon: LayoutGrid },
  { to: "/calculator", label: "Calculator", icon: Calculator },
  { to: "/tariffs", label: "Tariff Table", icon: Table },
  { to: "/graph", label: "Trends", icon: ChartSpline },
];

function classNames(...parts: Array<string | false | null | undefined>) {
  return parts.filter(Boolean).join(" ");
}

export default function Sidebar() {
  const [collapsed, setCollapsed] = React.useState(false); // desktop collapse
  const [mobileOpen, setMobileOpen] = React.useState(false); // mobile drawer
  const [isAuthenticated, setIsAuthenticated] = React.useState<boolean>(() => {
    // derive from token so we don't render auth-only UI without a token
    return !!localStorage.getItem("accessToken");
  });

  const location = useLocation();

  // Hide the sidebar entirely on the login page
  const onLoginPage = location.pathname.startsWith("/login");
  if (onLoginPage) return null;

  // Keep isAuthenticated in sync if other tabs log in/out
  React.useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === "accessToken") {
        setIsAuthenticated(!!localStorage.getItem("accessToken"));
      }
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  // Persist desktop collapse across reloads
  React.useEffect(() => {
    const saved = localStorage.getItem("sidebar:collapsed");
    if (saved) setCollapsed(saved === "1");
  }, []);
  React.useEffect(() => {
    localStorage.setItem("sidebar:collapsed", collapsed ? "1" : "0");
  }, [collapsed]);

  // Close mobile drawer when route changes
  React.useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  const ToggleIcon = collapsed ? PanelLeftOpen : PanelLeftClose;

  const handleLogout = () => {
    try {
      localStorage.removeItem("accessToken");
      setIsAuthenticated(false);
    } finally {
      // hard redirect to avoid stale auth state
      window.location.href = "/login";
    }
  };

  const SidebarInner = (
    <div
      className={classNames(
        "flex h-full flex-col border-r border-gray-200 bg-white/80 backdrop-blur supports-[backdrop-filter]:bg-white/60 transition-[width] duration-200",
        collapsed ? "w-20" : "w-72"
      )}
    >
      {/* Header / Brand */}
      <div className="flex h-16 items-center justify-between px-3">
        <div
          className={classNames(
            "flex items-center gap-2",
            collapsed && "justify-center w-full"
          )}
        >
          <div className="grid h-9 w-9 place-items-center rounded-2xl bg-gray-900 text-white font-bold">
            R
          </div>
          {!collapsed && (
            <span className="truncate text-lg font-semibold tracking-tight">
              RateWise
            </span>
          )}
        </div>
        {/* Collapse toggle (desktop only) */}
        <button
          onClick={() => setCollapsed((c) => !c)}
          className="hidden md:inline-flex items-center justify-center rounded-xl p-2 hover:bg-gray-100"
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          <ToggleIcon className="h-5 w-5" />
        </button>
        {/* Mobile close button */}
        <button
          onClick={() => setMobileOpen(false)}
          className="md:hidden inline-flex items-center justify-center rounded-xl p-2 hover:bg-gray-100"
          aria-label="Close sidebar"
        >
          <PanelLeftClose className="h-5 w-5" />
        </button>
      </div>

      {/* Nav */}
      <nav className="mt-2 flex-1 space-y-1 px-2">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === "/home"}
            title={label}
            aria-label={label}
            className={({ isActive }) =>
              classNames(
                "group flex items-center rounded-xl transition-colors",
                collapsed ? "justify-center p-2" : "gap-3 px-3 py-2",
                isActive
                  ? "bg-gray-900 text-white"
                  : "text-gray-700 hover:bg-gray-100"
              )
            }
          >
            <Icon className="h-5 w-5 shrink-0" />
            {!collapsed && <span className="truncate">{label}</span>}
          </NavLink>
        ))}
      </nav>

      {/* Footer / Auth */}
      <div className="border-t border-gray-200 p-2">
        {isAuthenticated && (
          <>
            <button
              onClick={handleLogout}
              className={classNames(
                "flex w-full items-center rounded-xl text-sm font-medium transition-colors",
                collapsed ? "justify-center p-2" : "gap-3 px-3 py-2",
                "border border-gray-300 hover:bg-gray-100"
              )}
              title="Logout"
              aria-label="Logout"
            >
              <LogOut className="h-5 w-5" />
              {!collapsed && <span>Logout</span>}
            </button>

            <button
              onClick={() => (window.location.href = "/profile")}
              className={classNames(
                "mt-2 flex w-full items-center rounded-xl text-sm font-medium transition-colors",
                collapsed ? "justify-center p-2" : "gap-3 px-3 py-2",
                "border border-gray-300 hover:bg-gray-100"
              )}
              title="Profile"
              aria-label="Profile"
            >
              <User className="h-5 w-5" />
              {!collapsed && <span>Profile</span>}
            </button>
          </>
        )}
      </div>
    </div>
  );

  return (
    <>
      {/* Mobile top bar with hamburger */}
      <div className="sticky top-0 z-40 flex h-14 items-center gap-2 border-b border-gray-200 bg-white/80 px-3 backdrop-blur md:hidden">
        <button
          onClick={() => setMobileOpen(true)}
          className="inline-flex items-center justify-center rounded-xl p-2 hover:bg-gray-100"
          aria-label="Open sidebar"
        >
          <PanelLeftOpen className="h-5 w-5" />
        </button>
        <span className="text-base font-semibold tracking-tight">RateWise</span>
      </div>

      {/* Desktop sidebar */}
      <aside className="relative hidden md:block">
        <div className="fixed inset-y-0 left-0 z-40">{SidebarInner}</div>
        {/* Spacer so content doesn't hide under fixed sidebar */}
        <div
          className={classNames(
            "hidden md:block transition-[width] duration-200",
            collapsed ? "w-20" : "w-72"
          )}
        />
      </aside>

      {/* Mobile drawer overlay */}
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
          <div className="absolute inset-y-0 left-0">{SidebarInner}</div>
        </div>
      )}
    </>
  );
}
