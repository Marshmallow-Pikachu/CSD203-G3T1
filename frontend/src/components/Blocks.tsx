export function RateBadge({ value }: { value: number }) {
  // slightly stronger color if rate is “high”
  const high = value >= 15;
  return (
    <span className={["inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold", high ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-700",].join(" ")}
      title="Rate percent">
      {value}% 
    </span>
  );
}

export function Badge({ children }: { children: React.ReactNode }) {
  return (
    <span className="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700">
      {children}
    </span>
  );
}


export function Row({label,children}: {label: string;children: React.ReactNode;}) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-slate-500">{label}</span>
      <span className="text-slate-800 font-medium">{children}</span>
    </div>
  );
}
