import Tile from "../components/buttons/Tile";
import { useNavigate } from "react-router-dom";
import { LayoutGrid, Calculator, Table, ChartSpline, Shield } from "lucide-react";

export default function Home() {
    const username = localStorage.getItem("username");
    const storedRole = localStorage.getItem("userRole");
    const navigate = useNavigate();

    let navItems = [
        { to: "/home", title: "Dashboard", icon: LayoutGrid },
        { to: "/calculator", title: "Calculator", icon: Calculator },
        { to: "/tariffs", title: "Tariffs", icon: Table },
        { to: "/graph", title: "Trends", icon: ChartSpline },
    ];
    if (storedRole === "ADMIN") {
        navItems.push({
            to: "/admin",
            title: "Admin Panel",
            icon: Shield,
        });
    }

    return (
        <section className="space-y-4">
            <h1 className="text-2xl font-semibold">Dashboard</h1>
            <p className="text-gray-600">
                Welcome to RateWise! {username} Use the sidebar to
                access the calculator and tariff data.
            </p>
            <div
                style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(3, 1fr)", // 3 tiles per row
                    gap: "24px",
                }}
            >
                {navItems.map(({ to, title, icon: Icon }) => (
                    <div
                        style={{
                            aspectRatio: "1 / 1",
                            width: "80%",
                        }}
                    >
                        <Tile
                            title={title}
                            onClick={() => {
                                if (to) {
                                     navigate(to);
                                }
                            }}
                            icon={Icon}
                        />
                    </div>
                ))}
            </div>
        </section>
    );
}