import LogoutButton from "./LogoutButton";

export default function Navbar() {

  return (
    <div className="w-full flex justify-between items-center p-4 bg-gray-100 shadow">
      <h1 className="text-xl font-bold">RateWise</h1>
      <LogoutButton></LogoutButton>
    </div>
  );
}