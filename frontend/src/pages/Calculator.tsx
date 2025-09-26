import CalculatorForm from "../components/CalculatorForm";
import CalculationResultPanel from "../components/CalculationResultPanel";
import React, { useState } from "react";


/*sample test data
{
  "productDescription": "Green coffee beans",
  "hsCode": "090111",
  "exporter": "SG",
  "importer": "CN",
  "agreement": "MFN",
  "goods_value": 1200,
  "quantity": 20,
  "freight": 180,
  "insurance": 60,
  "startDate": "2025-06-01",
  "endDate": "2025-06-30"
}
*/


export default function CalculatorPage() {
  const [calculationResult, setCalculationResult] = useState<any>(null);
  const [isCalculating, setIsCalculating] = useState(false);
  const [calculationError, setCalculationError] = useState<string | null>(null);

  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* Two-column layout; align both cards at the top */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left: form (2/3) inside a card */}
        <div className="lg:col-span-2">
          <div className="rounded-2xl border bg-white shadow-sm p-6">
            <CalculatorForm
              onCalculating={() => {
                setIsCalculating(true);
                setCalculationError(null);
              }}
              onResult={(data) => {
                setCalculationResult(data);
                setIsCalculating(false);
              }}
              onError={(message) => {
                setCalculationError(message || "Request failed");
                setIsCalculating(false);
              }}
            />
          </div>
        </div>

        {/* Right: result panel (1/3) inside a matching card */}
        <div className="lg:col-span-1">
          <div className="rounded-2xl border bg-white shadow-sm p-6">
            <CalculationResultPanel
              calculationResult={calculationResult}
              isCalculating={isCalculating}
              calculationError={calculationError}
            />
          </div>
        </div>
      </div>
    </div>
  );
}