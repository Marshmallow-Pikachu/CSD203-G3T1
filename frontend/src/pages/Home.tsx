

export default function Home() {
  const username = localStorage.getItem("username");

  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-semibold">Dashboard</h1>
      <p className="text-gray-600">
        Welcome to RateWise! {username} Use the sidebar to
        access the calculator and tariff data.
      </p>
      
    </section>
  );
}