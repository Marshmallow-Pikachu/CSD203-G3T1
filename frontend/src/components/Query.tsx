import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import { RateBadge, Badge, Row } from "./Blocks";

type Tariff = {
  hs_code: string;
  hs_description: string;
  exporter_code: string;
  exporter_name: string;
  importer_code: string;
  importer_name: string;
  agreement_code: string;
  agreement_name: string;
  rate_percent: number;
  customs_basis: string;
  valid_from?: string;         // added to match UI usage
  valid_to?: string | null;
};

export default function Query() {
  const [importerFilter, setImporterFilter] = useState<string>("");

  const { data, isLoading, error, refetch } = useQuery<Tariff[]>({
    queryKey: ["tariffs", importerFilter],
    queryFn: async () => {
      const res = await api.get("/api/v1/tariffs/list", {
        params: importerFilter ? { importer: importerFilter } : {},
      });
      return res.data as Tariff[];
    },
    enabled: true,
    staleTime: 30_000,
    retry: 1,
  });
  
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <label className="text-sm font-medium">Importer filter</label>
        <input
          type="text"
          placeholder="e.g. US"
          value={importerFilter}
          onChange={(e) => setImporterFilter(e.target.value.toUpperCase())}
          className="px-3 py-2 border rounded-md"
        />
        <button
          onClick={() => refetch()}
          className="px-3 py-2 bg-slate-900 text-white rounded-md"
        >
          Apply
        </button>
      </div>

      {isLoading && <div className="text-sm text-slate-500">Loading tariffs…</div>}
      {error && <div className="text-sm text-red-600">Failed to load tariffs.</div>}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {data?.map((t) => (
          <article
            key={`${t.hs_code}-${t.agreement_code}-${t.exporter_code}-${t.importer_code}`}
            className="rounded-lg border p-4 bg-white"
          >
            <div className="flex justify-between items-start mb-2">
              <div>
                <div className="text-xs text-gray-500">
                  {t.exporter_code} → {t.importer_code}
                </div>
                <div className="text-sm font-semibold">
                  {t.hs_code} — {t.hs_description}
                </div>
                <div className="text-xs text-gray-600">{t.agreement_name} ({t.agreement_code})</div>
              </div>

              <div className="text-right">
                <div className="text-xl font-extrabold">{t.rate_percent}%</div>
                <div className="text-xs text-gray-500">Rate</div>
              </div>
            </div>

            <div className="text-sm text-gray-700 space-y-1">
              <div>
                <span className="font-medium">Customs basis:</span>{" "}
                <span className="text-gray-600">{t.customs_basis ?? "—"}</span>
              </div>
              <div>
                <span className="font-medium">Valid from:</span>{" "}
                <span className="text-gray-600">{t.valid_from ?? "—"}</span>
              </div>
              <div>
                <span className="font-medium">Valid to:</span>{" "}
                <span className="text-gray-600">{t.valid_to ?? "—"}</span>
              </div>
            </div>
          </article>
        ))}

        {data && data.length === 0 && (
          <div className="text-sm text-gray-500">No tariffs found for the selected filter.</div>
        )}
      </div>
    </div>
  );
}