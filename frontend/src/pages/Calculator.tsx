import { useState } from "react";
import CalculatorForm from "../components/CalculatorForm";
import CalculationResultPanel from "../components/CalculationResultPanel";

// Same union the panel expects
type CalcTransportError =
  | {
      ok?: false;
      status?: number | null;
      message: string;
      hint?: string | null;
      raw?: any;
    }
  | string
  | null;

type CalculationResult = {
  duty?: number;
  tax?: number;
  customs_value?: number;
  rate_percent?: number;
  tax_rate_percent?: number;
  total_landed_cost?: number;
  exporter_input?: string;
  importer_input?: string;
  hs_code?: string;
  agreement?: string;
  customs_basis?: string;
  ok?: boolean;
  error?: string;
  hint?: string;
} | null;

// Convert any thrown error (Axios/fetch/custom) into what the panel can render
function normalizeError(err: any): CalcTransportError {
  if (!err) return "Request failed";
  if (typeof err === "string") return err;

  // Axios-style
  const status: number | null =
    (typeof err?.response?.status === "number" && err.response.status) || null;

  const backendMessage =
    err?.response?.data?.message ??
    err?.response?.data?.error ??
    err?.message ??
    "Unexpected error while calculating.";

  const hint: string | null =
    err?.response?.data?.hint ??
    (status === 400
      ? "Please check your inputs (HS code, countries, dates)."
      : status === 404
      ? "No tariff found for the given HS code/date range."
      : status === 422
      ? "Some fields are missing or invalid."
      : status && status >= 500
      ? "Server issue. Try again shortly."
      : null);

  return {
    ok: false,
    status,
    message: backendMessage,
    hint,
    raw: {
      request: {
        url: err?.config?.url,
        method: err?.config?.method,
        data: err?.config?.data,
      },
      response: err?.response?.data,
    },
  };
}

export default function CalculatorPage() {
  const [calculationResult, setCalculationResult] = useState<CalculationResult>(null);
  const [isCalculating, setIsCalculating] = useState(false);
  const [calculationError, setCalculationError] = useState<CalcTransportError>(null);

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left: form (2/3) */}
        <div className="lg:col-span-2">
          <div className="rounded-2xl border bg-white shadow-sm p-6">
            <CalculatorForm
              onCalculating={() => {
                setIsCalculating(true);
                setCalculationError(null);   // clear previous error
                setCalculationResult(null);  // optional: clear previous result
              }}
              onResult={(data) => {
                setCalculationResult(data as CalculationResult);
                setIsCalculating(false);
              }}
              onError={(err: any) => {
                setCalculationError(normalizeError(err));
                setIsCalculating(false);
              }}
            />
          </div>
        </div>

        {/* Right: result panel (1/3) */}
        <div className="lg:col-span-1">
          <div className="rounded-2xl border bg-white shadow-sm p-6">
            <CalculationResultPanel
              calculationResult={calculationResult}
              isCalculating={isCalculating}
              calculationError={calculationError} //
            />
          </div>
        </div>
      </div>
    </div>
  );
}
