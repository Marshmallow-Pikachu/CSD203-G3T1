import { api } from "./client";

export type CalcRequest = {
  exporter: string;
  importer: string;
  hsCode: string;
  agreement: string;      // e.g., "MFN" or "CPTPP"
  goods_value: number;    // make sure these are numbers, not strings
  quantity?: number;
  freight?: number;
  insurance?: number;
};

export async function calculateLandedCost(payload: CalcRequest) {
  // Your controller accepts a generic map, so this shape is fine.
  const { data } = await api.post("/api/v1/calculate/landed-cost", payload);
  return data as Record<string, unknown>;
}