import {api} from "./client";

export interface CalculateFields {
  productDescription: string;
  hsCode: string;
  exporter: string;
  importer: string;
  agreement: string;
  goods_value: number;
  quantity: number;
  freight: number;
  insurance: number;
  effectiveDate: string; // yyyy-MM-dd (input type="date")
}

/** Callbacks used by the parent page to show results in the right panel. */
export interface CalculatorFormProps {
  onCalculating?: (payload: any) => void;
  onResult?: (data: any) => void;
  onError?: (error: any) => void;
}

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

export type CalculationResult = {
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
export type CalcTransportError =
  | {
      ok?: false;
      status?: number | null;
      message: string;
      hint?: string | null;
      raw?: any;
    }
  | string
  | null;

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

// get all the countries for dropdown list
export async function fetchCountries() {
  const {data} = await api.get("/api/v1/countries"); // goes to CountryController in backend

  return data.map((countries: any) => ({
    value: countries.country_code,
    label: `${countries.country_name} (${countries.country_code})`
  }));
}

// get agreements for dropdown list
export async function fetchAgreements() {
  const {data} = await api.get("/api/v1/agreements"); // goes to AgreementController in backend

  return data.map((agreements: any) => ({
    value: agreements.agreement_code,
    label: agreements.agreement_code
  }));
}

// get hs codes description for dropdown list
export async function fetchHSCodesDescription() {
  const {data} = await api.get("/api/v1/hscodes"); // goes to HSCodeController in backend

  return data.map((hscodes: any) => ({
    value: hscodes.description,
    label: hscodes.description
  }));
}