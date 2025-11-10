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
    hs_description: string;
    rate_percent: number;
    valid_from: string;
    valid_to: string;
};

type TariffPoint = { date: string; rate: number };
type TariffData = { [pair: string]: TariffPoint[] };

// To reshape based on average rates
function reshapeData(data: Tariff[]): TariffData {
    const records: { [pair: string]: { [date:string]: number[] }} = {};

    // Process and store the rate values
    data.forEach(tariff => {
        const pair = `${tariff.exporter_code}→${tariff.importer_code}`;

        // If its the first time the pair is seen
        if (!records[pair]) {
            records[pair] = {};
        }

        // If its the first time the date is seen for the pair
        if (!records[pair][tariff.valid_from]) {
            records[pair][tariff.valid_from] = [];
        }

        // Add the rate into the records
        records[pair][tariff.valid_from].push(tariff.rate_percent);
    });

    const output: TariffData = {};

    Object.entries(records).forEach( ([pair, dateRates]) => {
        output[pair] = Object.entries(dateRates)
                             .map( ([date, rates]) => ({
                                date,
                                rate: rates.reduce( (sum, r) => sum + r, 0) / rates.length
                             }))
                             .sort( (a,b) => a.date.localeCompare(b.date));
    });

    return output;
}

// Helper to generate a random hex color
function getRandomColor() {
  const letters = '0123456789ABCDEF';
  let color = '#';
  for (let i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return color;
}

export default function Graph() {
    // For getting the data from the api
    const { data, isLoading, error } = useQuery<Tariff[]>({
        queryKey: ["tariffs", "all"],
        queryFn: async () => {
            const res = await api.get("/api/v1/tariffs/list");
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
    
    // clean and process the data
    const tariffData = reshapeData(data!);

    const labels: string[] = Array.from(
        new Set(
            Object.values(tariffData).flat().map(pt => pt.date)
        )
    ).sort();

    const datasets = Object.entries(tariffData).map(([pair, pairData]) => ({
        label: pair,
        data: labels.map(date => pairData.find(pt => pt.date === date)?.rate ?? null),
        fill: false,
        borderwidth: 2,
        borderColor: getRandomColor(),
        pointRadius: 3,
        pointHoverRadius: 4,
    }));

    const chartOptions = {
        responsive: true,
        plugins: { legend: {display: true}},
        scales: {
            x: {title: {display: true, text: "Date"}},
            y: {title: {display: true, text: "Average Tariff Rate (%"}}
        }
    }
    
    return (
        <section className="space-y-4">
            <h1 className="text-2xl font-semibold">Graph Page</h1>
            <p>This is a placeholder for the Graph page.</p>
            <Line 
                data={{labels, datasets}}
                options={chartOptions}
            />
        </section>
    );
}