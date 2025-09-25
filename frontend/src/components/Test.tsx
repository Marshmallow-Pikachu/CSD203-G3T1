import { useEffect, useState } from "react";
import { api } from "../api/client";

//Test Basic Connection to Backend with JWT

export default function Test() {
  const [countries, setCountries] = useState<any[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchCountries = async () => {
      try {
        const res = await api.get("/api/v1/countries");
        console.log("Countries response:", res.data);
        setCountries(res.data); // ✅ store countries in state
      } catch (err: any) {
        console.error("Failed to fetch countries:", err.response?.data || err.message);
        setError("Failed to fetch countries");
      }
    };

    fetchCountries(); // run once on component mount
  }, []);

  return (
    <div className="p-6">
      <h1 className="text-xl font-bold mb-4">Countries (JWT Test)</h1>
      {error && <p className="text-red-600">{error}</p>}
      {countries ? (
        <pre className="bg-gray-100 p-4 rounded text-sm overflow-x-auto">
          {JSON.stringify(countries, null, 2)}
        </pre>
      ) : (
        <p className="text-slate-500">Loading…</p>
      )}
    </div>
  );
}