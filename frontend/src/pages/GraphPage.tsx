import Graph from "../components/Graph";

export default function GraphPage() {
  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-semibold">Trends</h1>

      {/* Top: Table */}
      <div className="rounded-xl border border-gray-200 p-3">
        <Graph />
      </div>

    </section>
  );
}
