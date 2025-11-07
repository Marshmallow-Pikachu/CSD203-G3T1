import { PanelLeftClose, PanelLeftOpen } from "lucide-react";

export function SidebarHeader({ collapsed, setCollapsed }: any) {
  const ToggleIcon = collapsed ? PanelLeftOpen : PanelLeftClose;

  return (
    <div className="flex h-16 items-center justify-between px-3">
      <div
        className={`flex items-center gap-2 ${collapsed ? "justify-center w-full" : ""}`}
      >
        <div className="grid h-9 w-9 place-items-center rounded-2xl bg-gray-900 text-white font-bold">
          R
        </div>
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