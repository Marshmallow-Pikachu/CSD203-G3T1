import {api} from "./client";

export type CalculatorRequest = {
  exporter: string;
  importer: string;
  hsCode: string;
  agreement: string;
  goods_value: number;
  quantity?: number;
  freight?: number;
  insurance?: number;
  effectiveDate?: string | Date; // accept Date for UI convenience, will be normalized
};

// Helper: normalize effectiveDate to ISO yyyy-MM-dd (backend expects yyyy-MM-dd)
function toIsoDateString(d?: string | Date): string | undefined {
  if (d == null) return undefined;
  const dt = d instanceof Date ? d : new Date(d);
  if (Number.isNaN(dt.getTime())) {
    throw new Error("effectiveDate must be a valid date or ISO date string");
  }
  return dt.toISOString().slice(0, 10);
}

export async function calculateLandedCost(payload: CalculatorRequest) {
  // Ensure effectiveDate is sent as yyyy-MM-dd (string) if provided.
  const body = {
    ...payload,
    ...(payload.effectiveDate ? { effectiveDate: toIsoDateString(payload.effectiveDate) } : {}),
  };

  // Matches backend controller POST /api/v1/calculator/landed-cost
  const { data } = await api.post("/api/v1/calculator/landed-cost", body);
  return data as Record<string, unknown>;
}