import Query from "../components/Query"; 
import Table from "../components/Table";

export default function Home() {
  return (
    <section>
      <Table></Table>
    <Query country="US" />
    </section>
  );
}