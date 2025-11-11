import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import { Line } from "react-chartjs-2";
import {addMonths, isAfter, isBefore, format, parseISO } from "date-fns";
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

const MAX_MONTH_DISPLAY = 240;
const LABEL_LIST = ["SG→US", "US→SG", "SG→CN", "CN→SG","SG→JP", "JP→SG", "MY→SG", "VN→SG", "AU→SG", "AU→CN", "KR→SG"]

type TariffPoint = { date: string; rate: number | null};
type TariffData = { [pair: string]: TariffPoint[] };

interface Product {
    rate: number;
    valid_from: string;
}

// To get the range of months to display in our graph
function enumerateMonths(minDate: string, maxDate: string): string[] {
    const dates: string[] = [];
    let current = parseISO(minDate);
    const end = isBefore(addMonths(parseISO(maxDate),1), new Date()) ? parseISO(maxDate): new Date();
    while (isBefore(current, addMonths(end, 1))) {
        dates.push(format(current, "yyyy-MM"));
        current = addMonths(current, 1);
    }

    console.log(dates);
    const start = dates.length > MAX_MONTH_DISPLAY ? dates.length - MAX_MONTH_DISPLAY  : 0;
    return dates.slice(start);
}

// To reshape based on average rates
function reshapeData(data: Tariff[]): TariffData {
    // Find the bounds in the data
    const firstDate = data.filter(tariff => tariff.valid_from >= "2000-01-01")
                          .reduce((min, x) => min < x.valid_from ? min : x.valid_from, data[0].valid_from);
    const lastDate = data.reduce((max, x) => max > x.valid_to ? max : x.valid_to, data[0].valid_to);
    const monthList = enumerateMonths(firstDate, lastDate);

    const records: { [pair: string]: Tariff[] } = {};

    // Process and store the rate values
    data.filter(tariff => tariff.valid_from >= "2000-01-01")
    .forEach(tariff => {
        const pair = `${tariff.exporter_code}→${tariff.importer_code}`;

        // If its the first time the pair is seen
        if (!records[pair]) {
            records[pair] = [];
        }

        // Add the rate into the records
        records[pair].push(tariff);
    });
    
    // build the dataset for the graph
    const output: TariffData = {};

    for (const [pair, tariffs] of Object.entries(records)) {
        const monthSeries: TariffPoint[] = [];

        for (const month of monthList) {
            const products: { [hs: string]: Product } = {};

            tariffs.forEach( t => {
                const from = t.valid_from.slice(0, 7);
                const to = t.valid_to.slice(0,7);
                if (month >= from && month <= to) {
                    if (!products[t.hs_code] || new Date(t.valid_from) > new Date(products[t.hs_code].valid_from)) {
                        products[t.hs_code] = {rate: t.rate_percent, valid_from: t.valid_from};
                    }
                }
            });

            const rates = Object.values(products).map(p => p.rate);
            monthSeries.push({
                date: month,
                rate: rates.length > 0 ? rates.reduce((a,b) => a + b, 0) / rates.length : null,
            })
        }
        output[pair] = monthSeries;
    }


    return output;
}

// Helper to generate a random hex color
function getColor(pair: string) {
  switch (pair) {
    case "SG→US":
        return "#2ce1f9ff";
    case "US→SG":
        return "#1d68ffff";
    case "SG→CN":
        return "#ff9a9aff";
    case "CN→SG":
        return "#d92f2fff";
    case "SG→JP":
        return "#ff92e7ff";
    case "JP→SG":
        return "#b53fffff";
    case "MY→SG":
        return "#36fe8aff";
    case "VN→SG":
        return "#03762aff";
    case "AU→SG":
        return "#f9aa2cff";
    case "AU→CN":
        return "#ffecbbff";
    case "KR→SG":
        return "#3007d7ff";
    default:
        return "#000000";
  }
}

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
    
    // clean and process the data
    const tariffData = reshapeData(data!);

    const labels = Object.values(tariffData)[0]?.map((tp) => tp.date) ?? [];

    const datasets = Object.entries(tariffData).map(([pair, pairData]) => ({
        label: pair,
        data: pairData.map((pt) => pt.rate),
        fill: false,
        borderwidth: 2,
        borderColor: getColor(pair),
        pointRadius: 3,
        pointHoverRadius: 4,
    }));

    const chartOptions = {
        responsive: true,
        plugins: { 
            legend: { 
                display: true, 
                position: 'left',
                labels: {
                    boxWidth: 30,
                    boxHeight: 30,
                }
            },
            title: {
                display: true,
                text: 'Tariff changes in the past 20 years'
            }
        },
        scales: {
            x: {title: {display: true, text: "Date"}},
            y: {title: {display: true, text: "Average Tariff Rate (%)"}}
        },
        spanGaps: true,
    }
    
    return (
        <section className="space-y-4">
            <h1 className="text-2xl font-semibold">Historical Data</h1>
            <p>Below is the tariff changes in the past 20 years. You can click the labels on the left to toggle the visibility of each line.</p>
            <Line 
                data={{labels, datasets}}
                options={chartOptions}
            />
        </section>
    );
}
