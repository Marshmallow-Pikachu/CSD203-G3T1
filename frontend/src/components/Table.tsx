import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import { useNavigate } from "react-router-dom";

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
  tax_type: string;
  valid_from: string;
};

export default function Table() {
  // Start Coding Table here & call api to get data
  const navigate = useNavigate();

  const { data, isLoading, error } = useQuery<Tariff[]>({
    queryKey: ["tariffs", "all"],
    queryFn: async () => {
      const res = await api.get("/api/v1/tariffs/list");
      return res.data as Tariff[];
    },
    staleTime: 30_000,
    retry: 1,
  });

  if (isLoading) {
    return <div className="p-6 text-center text-slate-500">Loading…</div>;
  }

  if (error) {
    return (
      <div className="p-6 text-center text-red-600">
        Failed to load.
      </div>
    );
  }

  return (
    <>
      <div className="p-6 bg-gray-50">
        <div className="max-w-7xl mx-auto">
          <header className="mb-8 text-center space-y-2">
            <h1 className="text-2xl font-semibold text-slate-800">Tariff Table</h1>
          </header>

          <div className="bg-white rounded-lg border border-slate-200 p-5 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-100 border-b border-slate-500">
                  <tr className="text-base font-semibold text-slate-800">
                    <th className="px-6 py-4 text-left whitespace-nowrap">Exporter</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Importer</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Product</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">HS Code</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Tax Rule</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Agreement</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Rate (%)</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Custom Basis</th>
                    <th className="px-6 py-4 text-left whitespace-nowrap">Effective Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {data?.map((item) => (
                    <tr key={`${item.hs_code}-${item.importer_code}-${item.exporter_code}`} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.exporter_name} ({item.exporter_code})
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.importer_name} ({item.importer_code})
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.hs_description}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.hs_code}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        placeholder
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.agreement_name} ({item.agreement_code})
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.rate_percent}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.customs_basis}
                      </td>
                      <td className="px-6 py-4 text-left text-sm whitespace-nowrap">
                        {item.valid_from}
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