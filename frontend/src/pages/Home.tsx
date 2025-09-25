import Query from "../components/Query"; 
import Logout from "../components/Logout";
// import { Sidebar } from "../components/Sidebar";
// import Test from "../components/Test";
// import Countries from "./Countries";

export default function Home() {
  return (
    <section>
    <Query country="US" />
    <Logout/>
    {/* <Test /> */}
    </section>
  );
}