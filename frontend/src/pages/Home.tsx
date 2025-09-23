import Query from "../components/Query"; 
import { Sidebar } from "../components/Sidebar";

export default function Home() {
  return (
    <section>
      {/* <section className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
      <h1 className="text-4xl font-bold text-blue-600">Home Page</h1>
    </section> */}
    {/* <Sidebar /> */}
    <Query country="US" />
    </section>
  );
}