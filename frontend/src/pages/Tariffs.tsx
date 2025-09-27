// src/pages/Tariffs.tsx
import Query from "../components/Query";
import Table from "../components/Table";

export default function Tariffs() {
  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-semibold">Tariff Table</h1>

      {/* Top: Table */}
      <div className="rounded-xl border border-gray-200 p-3">
        <Table />
      </div>

      {/* Bottom: Query (hard-coded to US inside the component) */}
      <div className="rounded-xl border border-gray-200 p-3">
        <Query />
      </div>
    </section>
  );
}
