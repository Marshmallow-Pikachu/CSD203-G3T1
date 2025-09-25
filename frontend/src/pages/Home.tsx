import Query from "../components/Query"; 

export default function Home() {
  return (
    <section>
      <section className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
      <div className="p-6 bg-emerald-500 text-white text-2xl font-bold">
      Tailwind is working!
      </div>
      <h1 className="text-4xl font-bold text-blue-600">Home Page</h1>
      <p className="mt-4 text-lg text-gray-700">
        Welcome to my React + Tailwind app 🚀
      </p>
      <button className="mt-6 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">
        Click Me
      </button>
    </section>
    <Query />
    </section>
  );
}