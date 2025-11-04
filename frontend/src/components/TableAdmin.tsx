import { useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";

/* ========= Types from list endpoint ========= */
export type Tariff = {
  id?: string | number;            // if API returns id
  tariffId?: string | number;      // if API returns tariffId
  exporter_code: string;
  exporter_name: string;
  importer_code: string;
  importer_name: string;
  importer_customs: string;
  importer_tax: string;
  agreement_code: string;
  agreement_name: string;
  hs_code: string;
  hs_description: string;
  rate_percent: number;
  valid_from: string;
  valid_to: string | null;
};

/* ========= Small helpers ========= */
const fmtRate = (n: number | null | undefined) =>
  typeof n === "number" ? n.toLocaleString(undefined, { maximumFractionDigits: 2 }) : "—";

const fmtDate = (iso: string | null | undefined) => {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return new Intl.DateTimeFormat(undefined, { year: "numeric", month: "short", day: "2-digit" }).format(d);
};

/* ========= Admin API payloads ========= */
/** NOTE: Create does NOT include id */
type CreateTariffPayload = {
  exporterCode: string;
  importerCode: string;
  hsCode: string;
  agreementCode: string;
  ratePercent: number;
  validFrom: string;        // YYYY-MM-DD
  validTo: string | null;   // YYYY-MM-DD or null
};
/** Update uses the same payload, id goes in the URL */
type UpdateTariffPayload = CreateTariffPayload;

/* ========= Page ========= */
export default function TableAdmin() {
  const queryClient = useQueryClient();

  // Read-only list
  const { data, isLoading, isError, error, refetch, isFetching } = useQuery<Tariff[]>({
    queryKey: ["tariffs", "all"],
    queryFn: async () => {
      const res = await api.get("/api/v1/admin/tariffs");
      return res.data as Tariff[];
    },
    staleTime: 30_000,
    retry: 1,
  });

  // Keep id (id or tariffId) and flatten for the table
const rows = useMemo(() => {
  return (data ?? []).map((t) => {
    // pick up id no matter how backend names it
    const id =
      (t as any).id ??
      (t as any).tariffId ??
      (t as any).tariff_id ??
      undefined;

    return {
      id,
      exporter: `${t.exporter_name} (${t.exporter_code})`,
      exporterCode: t.exporter_code,
      importer: `${t.importer_name} (${t.importer_code})`,
      importerCode: t.importer_code,
      hs_code: t.hs_code,
      product: t.hs_description,
      rate_percent: t.rate_percent,
      agreement: `${t.agreement_name} (${t.agreement_code})`,
      agreementCode: t.agreement_code,
      importer_customs: t.importer_customs,
      importer_tax: t.importer_tax,
      valid_from: t.valid_from,
      valid_to: t.valid_to,
    };
  });
}, [data]);

  /* ====== Mutations (CRUD) ====== */

  // Create
  const createMutation = useMutation({
    mutationFn: async (payload: CreateTariffPayload) => {
      // (debug) show request
      alert(
        "📤 Sending HTTP Request:\n\n" +
          "POST /api/v1/admin/tariffs\n\n" +
          "Headers: { Content-Type: application/json }\n\n" +
          "Body:\n" +
          JSON.stringify(payload, null, 2)
      );
      const res = await api.post("/api/v1/admin/tariffs", payload, {
        headers: { "Content-Type": "application/json" },
      });
      alert("✅ Tariff Created:\n\n" + JSON.stringify(res.data, null, 2));
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tariffs", "all"] });
      setCreateOpen(false);
    },
    onError: (err: any) => {
      alert("❌ Error while creating tariff:\n\n" + (err?.message || JSON.stringify(err)));
    },
  });

  // Update
  const updateMutation = useMutation({
    mutationFn: async ({ id, payload }: { id: string | number; payload: UpdateTariffPayload }) => {
      const res = await api.put(`/api/v1/admin/tariffs/${id}`, payload, {
        headers: { "Content-Type": "application/json" },
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tariffs", "all"] });
      setEditOpen(false);
      setEditing(null);
    },
    onError: (err: any) => {
      alert("❌ Error while updating tariff:\n\n" + (err?.message || JSON.stringify(err)));
    },
  });

  // Delete
  const deleteMutation = useMutation({
    mutationFn: async (id: string | number) => {
      await api.delete(`/api/v1/admin/tariffs/${id}`);
      return true;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tariffs", "all"] });
    },
    onError: (err: any) => {
      alert("❌ Error while deleting tariff:\n\n" + (err?.message || JSON.stringify(err)));
    },
  });

  /* ===== UI state for modals ===== */
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editing, setEditing] = useState<{
    id: string | number;
    exporterCode: string;
    importerCode: string;
    hsCode: string;
    agreementCode: string;
    ratePercent: number;
    validFrom: string;
    validTo: string | null;
  } | null>(null);

  /* ===== Handlers ===== */
  const startCreate = () => setCreateOpen(true);

  const startEdit = (row: any) => {
    if (!row.id) {
      alert("This row has no id; cannot edit. Ensure your list endpoint returns an id.");
      return;
    }
    setEditing({
      id: row.id,
      exporterCode: row.exporterCode,
      importerCode: row.importerCode,
      hsCode: row.hs_code,
      agreementCode: row.agreementCode,
      ratePercent: Number(row.rate_percent) || 0,
      validFrom: row.valid_from?.slice(0, 10) ?? "",
      validTo: row.valid_to ? String(row.valid_to).slice(0, 10) : null,
    });
    setEditOpen(true);
  };

  const doDelete = (row: any) => {
    if (!row.id) {
      alert("This row has no id; cannot delete. Ensure your list endpoint returns an id.");
      return;
    }
    if (!confirm("Delete this tariff?")) return;
    deleteMutation.mutate(row.id);
  };

  /* ===== UI states ===== */
  if (isLoading) {
    return (
      <div className="p-6 bg-gray-50">
        <div className="max-w-7xl mx-auto">
          <header className="mb-8 text-center space-y-2">
            <h1 className="text-2xl font-semibold text-slate-800">Tariff Table</h1>
            <p className="text-slate-500">Loading data…</p>
          </header>
          <div className="bg-white rounded-lg border border-slate-200 p-5">
            <div className="animate-pulse space-y-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="h-6 bg-slate-100 rounded" />
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (isError) {
    const message = (error as Error)?.message ?? "Failed to load.";
    return (
      <div className="p-6 bg-gray-50">
        <div className="max-w-3xl mx-auto text-center">
          <h1 className="text-2xl font-semibold text-slate-800 mb-2">Tariff Table</h1>
          <div className="rounded-lg border border-red-200 bg-red-50 text-red-700 p-4">
            <p className="font-medium">Couldn’t fetch tariffs.</p>
            <p className="text-sm mt-1">{message}</p>
            <button
              onClick={() => refetch()}
              className="mt-4 inline-flex items-center rounded-md border px-3 py-1.5 text-sm font-medium hover:bg-slate-50"
            >
              Try again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 bg-gray-50">
      <div className="max-w-7xl mx-auto">
        <header className="mb-4 flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-slate-800">Tariff Table</h1>
          <div className="flex gap-3">
            <button
              onClick={startCreate}
              className="rounded-md bg-blue-600 text-white px-4 py-2 text-sm font-medium hover:bg-blue-700"
            >
              Add Tariff
            </button>
            <button
              onClick={() => refetch()}
              disabled={isFetching}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium hover:bg-slate-50"
            >
              {isFetching ? "Refreshing…" : "Refresh"}
            </button>
          </div>
        </header>

        <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
          <div className="overflow-x-auto max-h-[650px] overflow-y-auto">
            <table className="w-full border-collapse">
              <thead className="bg-gray-100 border-b border-slate-300 sticky top-0 z-10">
                <tr className="text-sm font-semibold text-slate-800">
                  <Th label="ID" />
                  <Th label="Exporter" />
                  <Th label="Importer" />
                  <Th label="HS Code" />
                  <Th label="Product" />
                  <Th label="Rate (%)" />
                  <Th label="Agreement" />
                  <Th label="Importer Customs" />
                  <Th label="Importer Tax" />
                  <Th label="Valid From" />
                  <Th label="Valid To" />
                  <Th label="Actions" />
                </tr>
              </thead>

              <tbody className="divide-y divide-gray-200 text-sm">
                {rows.length === 0 ? (
                  <tr>
                    <td colSpan={12} className="px-6 py-10 text-center text-slate-500">
                      No tariff rows found.
                    </td>
                  </tr>
                ) : (
                  rows.map((r, i) => (
                    <tr
                      key={(r.id ?? `${r.hs_code}-${r.importer}-${r.exporter}`) as string}
                      className={`transition-colors hover:bg-gray-50 ${i % 2 === 0 ? "bg-white" : "bg-gray-50/40"}`}
                    >
                      <Td mono>{r.id ?? "—"}</Td>
                      <Td>{r.exporter}</Td>
                      <Td>{r.importer}</Td>
                      <Td mono>{r.hs_code}</Td>
                      <Td>{r.product}</Td>
                      <Td mono>{fmtRate(r.rate_percent)}</Td>
                      <Td>{r.agreement}</Td>
                      <Td>{r.importer_customs}</Td>
                      <Td>{r.importer_tax}</Td>
                      <Td mono>{fmtDate(r.valid_from)}</Td>
                      <Td mono>{fmtDate(r.valid_to)}</Td>
                      <Td>
                        <div className="flex gap-2">
                          <button
                            className="rounded border border-slate-300 px-2 py-1 text-xs hover:bg-slate-50"
                            onClick={() => startEdit(r)}
                          >
                            Edit
                          </button>
                          <button
                            className="rounded border border-red-300 text-red-700 px-2 py-1 text-xs hover:bg-red-50"
                            onClick={() => doDelete(r)}
                          >
                            Delete
                          </button>
                        </div>
                      </Td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Create Modal */}
      {createOpen && (
        <Modal title="Add Tariff" onClose={() => setCreateOpen(false)}>
          <TariffForm
            onSubmit={(payload) => createMutation.mutate(payload)}
            submitting={createMutation.isPending}
          />
        </Modal>
      )}

      {/* Edit Modal */}
      {editOpen && editing && (
        <Modal title="Edit Tariff" onClose={() => { setEditOpen(false); setEditing(null); }}>
          <TariffForm
            initial={{
              exporterCode: editing.exporterCode,
              importerCode: editing.importerCode,
              hsCode: editing.hsCode,
              agreementCode: editing.agreementCode,
              ratePercent: editing.ratePercent,
              validFrom: editing.validFrom,
              validTo: editing.validTo,
            }}
            onSubmit={(payload) => updateMutation.mutate({ id: editing.id, payload })}
            submitting={updateMutation.isPending}
          />
        </Modal>
      )}
    </div>
  );
}

/* ========= Form ========= */
function TariffForm({
  initial,
  onSubmit,
  submitting,
}: {
  initial?: CreateTariffPayload;
  onSubmit: (p: CreateTariffPayload) => void;
  submitting?: boolean;
}) {
  const [form, setForm] = useState<CreateTariffPayload>(
    initial ?? {
      exporterCode: "",
      importerCode: "",
      hsCode: "",
      agreementCode: "",
      ratePercent: 0,
      validFrom: "",
      validTo: null,
    }
  );

  const update = (k: keyof CreateTariffPayload, v: any) =>
    setForm((s) => ({ ...s, [k]: v }));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      ...form,
      ratePercent: Number(form.ratePercent),
      validTo: form.validTo === "" ? null : form.validTo,
    });
  };

  return (
    <form onSubmit={submit} className="space-y-3">
      <Row>
        <Field
          label="Exporter Code"
          value={form.exporterCode}
          onChange={(e) => update("exporterCode", e.target.value)}
          placeholder="e.g., SG, AU"
          required
        />
        <Field
          label="Importer Code"
          value={form.importerCode}
          onChange={(e) => update("importerCode", e.target.value)}
          placeholder="e.g., JP, SG"
          required
        />
      </Row>

      <Row>
        <Field
          label="HS Code"
          value={form.hsCode}
          onChange={(e) => update("hsCode", e.target.value)}
          placeholder="e.g., 847130"
          required
        />
        <Field
          label="Agreement Code"
          value={form.agreementCode}
          onChange={(e) => update("agreementCode", e.target.value)}
          placeholder="e.g., AANZFTA"
          required
        />
      </Row>

      <Row>
        <Field
          label="Rate Percent"
          type="number"
          step="0.01"
          value={form.ratePercent}
          onChange={(e) => update("ratePercent", e.target.value)}
          placeholder="e.g., 2.5"
          required
        />
        <Field
          label="Valid From"
          type="date"
          value={form.validFrom}
          onChange={(e) => update("validFrom", e.target.value)}
          required
        />
        <Field
          label="Valid To"
          type="date"
          value={form.validTo ?? ""}
          onChange={(e) => update("validTo", e.target.value)}
        />
      </Row>

      <div className="flex justify-end gap-2 pt-2">
        <button
          type="submit"
          disabled={submitting}
          className="rounded-md bg-blue-600 text-white px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
        >
          {submitting ? "Saving…" : "Save"}
        </button>
      </div>
    </form>
  );
}

/* ========= Presentational helpers ========= */
function Th({ label }: { label: string }) {
  return (
    <th scope="col" className="px-6 py-3 text-left whitespace-nowrap select-none">
      {label}
    </th>
  );
}

function Td({ children, mono = false }: { children: React.ReactNode; mono?: boolean }) {
  return <td className={`px-6 py-3 text-left whitespace-nowrap ${mono ? "font-mono" : ""}`}>{children}</td>;
}

function Row({ children }: { children: React.ReactNode }) {
  return <div className="grid grid-cols-1 md:grid-cols-3 gap-3">{children}</div>;
}

function Field({
  label,
  type = "text",
  value,
  onChange,
  placeholder,
  required,
  step,
}: {
  label: string;
  type?: string;
  value: any;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string;
  required?: boolean;
  step?: string;
}) {
  return (
    <label className="block text-sm">
      <span className="block text-slate-600 mb-1">{label}</span>
      <input
        type={type}
        step={step}
        value={value ?? ""}
        onChange={onChange}
        placeholder={placeholder}
        required={required}
        className="w-full rounded-md border border-slate-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
    </label>
  );
}

function Modal({
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