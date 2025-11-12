import { PanelLeftClose, PanelLeftOpen } from "lucide-react";
import Logo from "../../../assets/RateWise.svg?react";

export function SidebarHeader({ collapsed, setCollapsed }: any) {
  const ToggleIcon = collapsed ? PanelLeftOpen : PanelLeftClose;

  return (
    <div className="flex h-16 items-center justify-between px-3">
      <div
        className={`flex items-center gap-2 ${collapsed ? "justify-center w-full" : ""}`}
      >
        <Logo className="size-8" />
        {!collapsed && <span className="text-lg font-semibold">RateWise</span>}
      </div>

      <button
        onClick={() => setCollapsed((c: boolean) => !c)}
        className="hidden md:flex items-center justify-center rounded-xl p-2 hover:bg-gray-100"
        aria-label="Toggle sidebar"
      >
        <ToggleIcon className="h-5 w-5" />
      </button>
    </div>
  );
}