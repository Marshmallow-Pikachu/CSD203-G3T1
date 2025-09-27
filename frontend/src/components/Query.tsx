import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import { RateBadge, Badge, Row } from "./Blocks";
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
};

export default function Query() {
  const navigate = useNavigate();

  const { data, isLoading, error } = useQuery<Tariff[]>({
    queryKey: ["tariffs", "US"],
    queryFn: async () => {
      const res = await api.get("/api/v1/tariffs/list", {
        params: { importer: "US" },
      });
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
    <div className="p-6 bg-gray-50">
      <div className="max-w-7xl mx-auto">
        <header className="mb-8 text-center space-y-2">
          <h1 className="text-2xl font-semibold text-slate-800">Tariff List</h1>
          <p className="text-sm text-slate-500">
            Importer: <span className="font-medium">US</span>
          </p>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {data?.map((item) => (
            <article
              key={`${item.hs_code}-${item.agreement_code}`}
              className="rounded-xl border border-slate-200 bg-white p-5 shadow-2sm hover:shadow-md transition cursor-pointer"
              onClick={() => navigate(`/hs/${item.hs_code}`)}
            >
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs text-slate-500 font-medium">
                  HS: <span className="font-mono pr-4">{item.hs_code}</span>
                </span>
                <div className="flex items-center gap-2">
                  <Badge>
                    {item.agreement_name} ({item.agreement_code})
                  </Badge>
                  <RateBadge value={item.rate_percent} />
                </div>
              </div>

              <h2 className="text-base font-semibold text-slate-800">
                {item.hs_description}
              </h2>

              <div className="mt-4 grid grid-cols-1 gap-2 text-sm">
                <Row label="Importer">
                  {item.importer_name} ({item.importer_code})
                </Row>
                <Row label="Exporter">
                  {item.exporter_name} ({item.exporter_code})
                </Row>
                <Row label="Customs Basis">{item.customs_basis}</Row>
              </div>
            </article>
          ))}
        </div>
      </div>
    </div>
  );
}
