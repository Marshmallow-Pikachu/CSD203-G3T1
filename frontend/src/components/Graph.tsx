import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import { Line } from "react-chartjs-2";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip,
  Legend,
  plugins,
  TimeScale
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
);

type Tariff = {
    exporter_code: string;
    exporter_name: string;
    importer_code: string;
    importer_name: string;
    importer_customs: string;
    importer_tax: string;
    agreement_code: string;
    agreement_name: string;
    hs_code: string;
    hs_description: string; // product
    rate_percent: number; // tariff rate
    valid_from: string;
    valid_to: string; // newly added column
};

export default function Graph() {
    // For getting the data from the api
    const { data, isLoading, error } = useQuery<Tariff[]>({
        queryKey: ["tariffs", "all"],
        queryFn: async () => {
            const res = await api.get("/api/v1/tariffs/table");
            return res.data as Tariff[];
        },
        staleTime: 30_000,
        retry: 1,
    });

    if (isLoading) {
        return <div className="p-6 text-center text-slate-500">Loading…</div>;
    }

    if (error) {
        return (
            <div className="p-6 text-center text-red-600">
                Failed to load.
            </div>
        );
    }

    const hsCodes = Array.from(new Set(data?.map(tariff => tariff.hs_code)));

    const datasets = hsCodes.map(hsCode => {
        const items = data?.filter(tariff => tariff.hs_code === hsCode);
        
        return {
            label: items?.[0].hs_description || hsCode,
            data: items?.map(tariff => ({
                x: tariff.valid_to,
                y: tariff.rate_percent
            })) || [],
            fill: false,
            borderColor: '#' + Math.floor(Math.random() * 16777215).toString(16),
            spanGaps: true,
        }
    })
    const chartOptions = {
        responsive: true,
        scales: {
            x: {
                type: 'time',
                time: { unit: 'year' },
                title: { display: true, text: 'Date' }
            },
            y: { beginAtZero: true, title: { display: true, text: 'Tariff Rate (%)' } },
        },
        plugins: {
            legend: { display: true },
            title: { display: true, text: 'Tariff Rates Over Time' }
        }
    };

    return (
        <section className="space-y-4">
            <h1 className="text-2xl font-semibold">Graph Page</h1>
            <p>This is a placeholder for the Graph page.</p>
            <Line 
                data={{datasets}}
                options={{chartOptions}}
            />
        </section>
    );
}