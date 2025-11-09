import TableAdmin from "../components/TableAdmin";

export default function Admin() {
  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-semibold">Admin</h1>
      <div className="rounded-xl border border-gray-200 p-3">
        <TableAdmin />
      </div>
    </section>
  );
}
