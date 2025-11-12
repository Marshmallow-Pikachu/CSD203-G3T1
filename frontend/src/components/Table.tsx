import { useQuery } from "@tanstack/react-query";
// For search bar
import { useState } from "react";
import AutocompleteSearch from "./AutocompleteSearch";
import { fetchTariffsTable, type TariffRow } from "../api/table";

export default function Table() {
  const { data, isLoading, error } = useQuery<TariffRow[]>({
    queryKey: ["tariffs", "all"],
    queryFn: fetchTariffsTable,
    staleTime: 30_000,
    retry: 1,
  });

  const [filteredData, setFilteredData] = useState<TariffRow[]>([]);
  const displayData = (filteredData.length ? filteredData : data) ?? [];

  if (isLoading) return <div className="p-6 text-center text-slate-500">Loading…</div>;
  if (error) return <div className="p-6 text-center text-red-600">Failed to load.</div>;


  return (
    <>
      <div className="p-6 bg-gray-50">
        <div className="max-w-7xl mx-auto">
          <header className="mb-8 text-center space-y-2">
            <h1 className="text-2xl font-semibold text-slate-800">Tariff Table</h1>
          </header>

          {/* Search bar */}
          <div className="flex justify-center">
            <AutocompleteSearch
              data={data || []}
              searchFields={[
                "exporter_name",
                "importer_name",
                "hs_code",
                "hs_description",
                "agreement_name",
              ]}
              onFilter={setFilteredData}
              placeholder="Search by exporter, importer, HS code, product, or agreement..."
            />
          </div>

          <div className="bg-white rounded-lg border border-slate-200 p-5 overflow-hidden">
            <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
              <table className="w-full">
                <thead className="bg-gray-100 border-b border-slate-500">
                  <tr className="text-base font-semibold text-slate-800">
                    <th className="px-6 py-4 text-left whitespace-nowrap">Exporter</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Importer</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">HS Code</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Product</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Rate (%)</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Agreement</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Importer Customs</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Importer Tax</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Valid From</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Valid To</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {displayData?.map((item, idx) => (
                    <tr key={`${item.hs_code}-${item.importer_code}-${item.exporter_code}-${idx}`} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.exporter_name} ({item.exporter_code})
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.importer_name} ({item.importer_code})
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.hs_code}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.hs_description}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.rate_percent}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.agreement_name} ({item.agreement_code})
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.importer_customs}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.importer_tax}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.valid_from}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.valid_to ?? "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

    </>
  )
}