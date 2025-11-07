import React, { useState } from "react";

type CreateTariffPayload = {
  exporterCode: string;
  importerCode: string;
  hsCode: string;
  agreementCode: string;
  ratePercent: number;
  validFrom: string;        // YYYY-MM-DD
  validTo: string | null;   // YYYY-MM-DD or null
};

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


export default function TariffForm({
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
