import { NavLink } from "react-router-dom";
import { LayoutGrid, Calculator, Table, ChartSpline, Shield } from "lucide-react";

const baseNavItems = [
  { to: "/home", label: "Dashboard", icon: LayoutGrid },
  { to: "/calculator", label: "Calculator", icon: Calculator },
  { to: "/tariffs", label: "Tariffs", icon: Table },
  { to: "/graph", label: "Trends", icon: ChartSpline },
];

export function SidebarNav({ collapsed }: { collapsed: boolean }) {
  const storedRole = localStorage.getItem("userRole");
  let navItems = [...baseNavItems];
  // Use an if-statement to append the admin link if needed
  if (storedRole === "ADMIN") {
    navItems.push({
      to: "/admin",
      label: "Admin Panel",
      icon: Shield,
    });
  }

  return (
    <nav className="mt-2 flex-1 space-y-1 px-2">
      {navItems.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          end={to === "/home"}
          className={({ isActive }) =>
            [
              "flex items-center rounded-xl transition-colors",
              collapsed ? "justify-center p-2" : "gap-3 px-3 py-2",
              isActive
                ? "bg-gray-900 text-white"
                : "text-gray-700 hover:bg-gray-100",
            ].join(" ")
          }
        >
          <Icon className="h-5 w-5 shrink-0" />
          {!collapsed && <span>{label}</span>}
        </NavLink>
      ))}
    </nav>
  );
}