/* ========= Presentational helpers ========= */
export function Th({ label }: { label: string }) {
  return (
    <th scope="col" className="px-6 py-3 text-left whitespace-nowrap select-none">
      {label}
    </th>
  );
}

export function Td({ children, mono = false }: { children: React.ReactNode; mono?: boolean }) {
  return <td className={`px-6 py-3 text-left whitespace-nowrap ${mono ? "font-mono" : ""}`}>{children}</td>;
}


export function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: React.ReactNode;
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl p-5">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold text-slate-800">{title}</h2>
          <button onClick={onClose} className="rounded-md border px-2 py-1 text-sm hover:bg-slate-50">
            Close
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}