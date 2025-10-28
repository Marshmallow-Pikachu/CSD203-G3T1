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
  effectiveDate?: string; // show effective date returned by backend
  chosenTariff?: { rate?: number; source?: string }; // debug info
  ok?: boolean;
  error?: string;
  hint?: string;
} | null;

// Transport-layer / interceptor-normalized error (object) OR legacy string
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

const formatUSD = (value?: number) =>
  typeof value === "number"
    ? value.toLocaleString("en-US", { style: "currency", currency: "USD" })
    : "—";

const formatPercent = (value?: number) =>
  typeof value === "number" ? `${value}%` : "—";

export default function CalculationResultPanel({
  calculationResult,
  isCalculating,
  calculationError,
}: {
  calculationResult: CalculationResult;
  isCalculating?: boolean;
  calculationError?: CalcTransportError;
}) {
  const duty = calculationResult?.duty ?? 0;
  const tax = calculationResult?.tax ?? 0;
  const customsValue = calculationResult?.customs_value ?? 0;
  const dutyRate = calculationResult?.rate_percent;
  const taxRate = calculationResult?.tax_rate_percent;
  const totalDutiesAndTaxes = duty + tax;
  const totalLandedCost =
    typeof calculationResult?.total_landed_cost === "number"
      ? calculationResult.total_landed_cost
      : customsValue + duty + tax;

  const renderTransportError = (err: CalcTransportError) => {
    if (!err) return null;
    if (typeof err === "string") {
      return (
        <div className="rounded-lg border border-red-300 bg-red-50 text-red-700 p-3 mb-4">
          {err}
        </div>
      );
    }
    return (
      <div className="rounded-lg border border-red-300 bg-red-50 text-red-700 p-3 mb-4">
        <p className="font-semibold">
          {typeof err.status === "number" ? `Error ${err.status}` : "Error"}
        </p>
        <p className="text-sm mt-0.5">{err.message}</p>
        {err.hint && (
          <p className="text-xs mt-1 opacity-90">
            <span className="font-semibold">Hint:</span> {err.hint}
          </p>
        )}
      </div>
    );
  };

  return (
    <aside>
      <h3 className="text-2xl font-extrabold mb-4">Calculation Result</h3>

      {isCalculating && (
        <div className="animate-pulse space-y-4">
          <div className="h-20 rounded-xl bg-gray-100" />
          <div className="h-20 rounded-xl bg-gray-100" />
          <div className="h-20 rounded-xl bg-gray-100" />
          <div className="h-32 rounded-xl bg-gray-100" />
        </div>
      )}

      {!isCalculating && calculationError && renderTransportError(calculationError)}

      {!isCalculating && !calculationError && calculationResult?.ok === false && (
        <div className="rounded-lg border border-amber-300 bg-amber-50 text-amber-800 p-3 mb-4">
          <p className="font-semibold">Validation error</p>
          <p className="text-sm">{calculationResult?.error}</p>
          {calculationResult?.hint && (
            <p className="text-xs mt-1 opacity-80">{calculationResult.hint}</p>
          )}
        </div>
      )}

      {!isCalculating && !calculationError && !calculationResult && (
        <p className="text-gray-500">
          Fill out the form and click <em>Calculate</em> to see results.
        </p>
      )}

      {!isCalculating &&
        !calculationError &&
        calculationResult &&
        calculationResult.ok !== false && (
          <div className="space-y-4">
            <div className="flex flex-wrap gap-2 text-xs">
              {calculationResult.exporter_input && (
                <span className="px-2 py-1 rounded-full bg-gray-100 border">
                  From: {calculationResult.exporter_input}
                </span>
              )}
              {calculationResult.importer_input && (
                <span className="px-2 py-1 rounded-full bg-gray-100 border">
                  To: {calculationResult.importer_input}
                </span>
              )}
              {calculationResult.hs_code && (
                <span className="px-2 py-1 rounded-full bg-gray-100 border">
                  HS: {calculationResult.hs_code}
                </span>
              )}
              {calculationResult.agreement && (
                <span className="px-2 py-1 rounded-full bg-gray-100 border">
                  Agreement: {calculationResult.agreement}
                </span>
              )}
              {calculationResult.customs_basis && (
                <span className="px-2 py-1 rounded-full bg-gray-100 border">
                  Basis: {calculationResult.customs_basis}
                </span>
              )}
              {calculationResult.effectiveDate && (
                <span className="px-2 py-1 rounded-full bg-gray-100 border">
                  As of: {calculationResult.effectiveDate}
                </span>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="rounded-xl border p-4">
                <div className="font-semibold">Import Duty</div>
                <div className="text-lg">
                  {formatUSD(duty)}{" "}
                  <span className="text-sm text-gray-500">({formatPercent(dutyRate)})</span>
                </div>
              </div>

              <div className="rounded-xl border p-4">
                <div className="font-semibold">VAT / Tax</div>
                <div className="text-lg">
                  {formatUSD(tax)}{" "}
                  <span className="text-sm text-gray-500">({formatPercent(taxRate)})</span>
                </div>
              </div>
            </div>

            <div className="rounded-xl border p-4">
              <div className="font-semibold">Total Duties + Taxes</div>
              <div className="text-lg">{formatUSD(totalDutiesAndTaxes)}</div>
            </div>

            <div className="rounded-xl border p-4">
              <div className="font-semibold mb-2">Total Landed Cost</div>
              <div className="text-2xl font-extrabold">{formatUSD(totalLandedCost)}</div>

              <div className="mt-3 text-sm text-gray-700 space-y-1">
                <div className="flex justify-between">
                  <span>Value of Goods</span>
                  <span className="font-medium">{formatUSD(customsValue)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Duty</span>
                  <span className="font-medium">{formatUSD(duty)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Tax</span>
                  <span className="font-medium">{formatUSD(tax)}</span>
                </div>
                {calculationResult.chosenTariff && (
                  <div className="mt-3 text-xs text-gray-600">
                    <div>Tariff chosen: {calculationResult.chosenTariff.rate ?? "—"}%</div>
                    <div className="opacity-80 text-xs">Source: {calculationResult.chosenTariff.source ?? "—"}</div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
    </aside>
  );
}