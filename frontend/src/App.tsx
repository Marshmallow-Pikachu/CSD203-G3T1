import { useQuery } from "@tanstack/react-query";
import { api } from "./api/client";

export default function App() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["ping"],
    queryFn: async () => {
      const response = await api.get("/db/ping");  // Axios call
      console.log("Full Axios Response:", response); // 👈 print everything
      return response; // return whole response, not just .data
    },
  });

  if (isLoading) return <p>Loading...</p>;
  if (error) return <p>Error: {String(error)}</p>;

  return (
    <div>
      <h1>Backend Ping Response:</h1>
      <pre>{JSON.stringify(data, null, 2)}</pre> {/* pretty print response */}
    </div>
  );
}