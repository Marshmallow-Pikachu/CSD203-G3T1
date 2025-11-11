import { useForm } from "react-hook-form";
import { useMutation } from "@tanstack/react-query";
import InputField from "./InputField";
import { calculateLandedCost } from "../api/calculator";
// For dropdown
import { useQuery } from "@tanstack/react-query";
import { fetchCountries , fetchAgreements} from "../api/calculator";
import SelectField from "./SelectField";

/**
 * Form fields now use a single effectiveDate (ISO date string) instead of startDate/endDate.
 */
interface CalculateFields {
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

export default function CalculatorForm({
  onCalculating,
  onResult,
  onError,
}: CalculatorFormProps) {
  const { handleSubmit, register } = useForm<CalculateFields>({
    defaultValues: {
      productDescription: "",
      hsCode: "",
      exporter: "",
      importer: "",
      agreement: "MFN",
      goods_value: 0,
      quantity: 1,
      freight: 0,
      insurance: 0,
      effectiveDate: "",
    },
  });

  const mutation = useMutation({
    mutationFn: (body: any) => calculateLandedCost(body),
    onSuccess: (data) => onResult?.(data),
    onError: (err: any) => onError?.(err),
  });

  // goes to api/calculator to fetchCountries for dropdown list
  const {
    data: countries
  } = useQuery({
    queryKey: ["countries"],
    queryFn: fetchCountries,
  });

  // goes to api/calculator to fetchAgreement for dropdown list
  const {
    data: agreements
  } = useQuery({
    queryKey: ["agreements"],
    queryFn: fetchAgreements,
  });

  // Ensure date string (from <input type="date">) is normalized to yyyy-MM-dd
  const normalizeDateInput = (d?: string) => {
    // input type="date" already gives 'YYYY-MM-DD' in most browsers; just sanity-check it
    if (!d) return undefined;
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(d.trim());
    return m ? `${m[1]}-${m[2]}-${m[3]}` : undefined;
  };

  const submit = (raw: CalculateFields) => {
    // helpers
    const s = (x?: string) => (x && x.trim() !== "" ? x.trim() : undefined);

    const hs = s(raw.hsCode);
    const payload: any = {
      exporter: s(raw.exporter),
      importer: s(raw.importer),
      agreement: s(raw.agreement) ?? "MFN",
      hsCode: hs,s,
      // only send productDescription if hsCode is missing
      productDescription: hs ? undefined : s(raw.productDescription),
      goods_value: Number.isFinite(raw.goods_value) ? Number(raw.goods_value) : 0,
      quantity: Number.isFinite(raw.quantity) ? Number(raw.quantity) : 1,
      freight: Number.isFinite(raw.freight) ? Number(raw.freight) : 0,
      insurance: Number.isFinite(raw.insurance) ? Number(raw.insurance) : 0,
    };

    const eff = normalizeDateInput(raw.effectiveDate);
    if (eff) payload.effectiveDate = eff;

    onCalculating?.(payload);
    mutation.mutate(payload);
  };

  return (
    <form onSubmit={handleSubmit(submit)}>
      <h2 className="text-3xl font-bold mb-6">Calculator</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Left column */}
        <div className="space-y-4">
          <InputField label="Product Description" name="productDescription" register={register} />
          {/* <InputField label="Country of Origin" name="exporter" register={register} /> */}
          <SelectField
            label="Country of Origin"
            name="exporter"
            register={register}
            choices={countries || []}
          />
          <InputField
            label="Product Value (USD)"
            name="goods_value"
            type="number"
            register={register}
            options={{ valueAsNumber: true }}
          />
          {/* <InputField label="Agreement" name="agreement" register={register} /> */}
          <SelectField
            label="Agreement"
            name="agreement"
            register={register}
            choices={agreements || []}
          />
          <InputField
            label="Product Quantity"
            name="quantity"
            type="number"
            register={register}
            options={{ valueAsNumber: true }}
          />
        </div>

        {/* Right column */}
        <div className="space-y-4">
          <InputField label="HS Code" name="hsCode" register={register} />
          {/* <InputField label="Destination Country" name="importer" register={register} /> */}
          <SelectField
            label="Destination Country"
            name="importer"
            register={register}
            choices={countries || []}
          />
          <InputField
            label="Freight/Shipping Cost (USD)"
            name="freight"
            type="number"
            register={register}
            options={{ valueAsNumber: true }}
          />
          <InputField
            label="Insurance"
            name="insurance"
            type="number"
            register={register}
            options={{ valueAsNumber: true }}
          />

          <div>
            <InputField
              label="Effective Date"
              name="effectiveDate"
              type="date"
              register={register}
            />
          </div>
        </div>
      </div>

      <button
        id="calculate-button"
        type="submit"
        disabled={mutation.isPending}
        className="mt-6 w-full md:w-auto px-6 py-3 rounded-xl border bg-gray-900 text-white hover:bg-gray-800 disabled:opacity-60"
      >
        {mutation.isPending ? "Calculating..." : "Calculate Landed Cost"}
      </button>
    </form>
  );
}