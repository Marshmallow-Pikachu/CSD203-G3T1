import React, { useState } from "react";
import { useForm } from "react-hook-form";
import { useMutation } from "@tanstack/react-query";
import InputField from "./InputField";
import { calculateLandedCost } from "../api/calculator";


/**
 * - This interface defines the exact shape of the form data managed by react-hook-form.
 * - The keys here must match the "name" props passed to <InputField>.
 * - Using camelCase (startDate, endDate) ensures consistency across frontend code.
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
  startDate: string;     
  endDate: string;
}

/** Callbacks used by the parent page to show results in the right panel. */
export interface CalculatorFormProps {
  onCalculating?: (payload: any) => void;
  onResult?: (data: any) => void;
  onError?: (message?: string) => void;
}

/**
 * CalculatorForm component
 *
 * - Collects user input using react-hook-form
 * - Converts raw values into a payload for the backend
 * - Calls calculateLandedCost() (API wrapper) with useMutation
 * - Shows loading state, errors, and the backend's JSON result
 */
export default function CalculatorForm({
  onCalculating,
  onResult,
  onError,
}: CalculatorFormProps) {
  /**
   * react-hook-form setup
   * - Manages all inputs and validation.
   * - defaultValues ensures controlled inputs.
   */
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
      startDate: "",
      endDate: "",
    },
  });

  // Local state to display the backend's response
  const [result, setResult] = useState<any>(null);

  /**
 * React Query Mutation
 * - Wraps the API call in a mutation for async state (loading, error, success).
 * - calculateLandedCost is defined in ../api/calculator.ts and calls backend.
 * - onSuccess: pass data up to parent via onResult callback.
 * - onError: pass error message up to parent via onError callback.
 */
 const mutation = useMutation({
    mutationFn: (body: any) => calculateLandedCost(body),
    onSuccess: (data) => onResult?.(data),
    onError: (e: any) => onError?.(e?.message || "Request failed"),
  });

   /**
   * Submit handler
   * - Converts raw form data into backend payload.
   * - Ensures numeric fields are numbers, not strings.
   * - Calls onCalculating before sending request.
   * - Triggers the mutation (async POST).
   */
  const submit = (raw: CalculateFields) => {
    const payload = {
      exporter: raw.exporter,
      importer: raw.importer,
      hsCode: raw.hsCode,
      agreement: raw.agreement,
      goods_value: Number(raw.goods_value),
      quantity: Number(raw.quantity),
      freight: Number(raw.freight),
      insurance: Number(raw.insurance),
      startDate: raw.startDate || undefined,
      endDate: raw.endDate || undefined,
      productDescription: raw.productDescription || undefined,
    };
    onCalculating?.(payload);
    mutation.mutate(payload);
  };

  // JSX layout
  // Render the form, using InputField for each input.
  // Button state, error messages, and result JSON are shown below.

  return (
    <form onSubmit={handleSubmit(submit)}>
      <h2 className="text-3xl font-bold mb-6">Calculator</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Left column */}
        <div className="space-y-4">
          <InputField label="Product Description" name="productDescription" register={register} />
          <InputField label="Country of Origin" name="exporter" register={register} />
          <InputField label="Product Value (USD)" name="goods_value" type="number" register={register} options={{ valueAsNumber: true }} />
          <InputField label="Agreement" name="agreement" register={register} />
          <InputField label="Product Quantity" name="quantity" type="number" register={register} options={{ valueAsNumber: true }} />
        </div>

        {/* Right column */}
        <div className="space-y-4">
          <InputField label="HS Code" name="hsCode" register={register} />
          <InputField label="Destination Country" name="importer" register={register} />
          <InputField label="Freight/Shipping Cost (USD)" name="freight" type="number" register={register} options={{ valueAsNumber: true }} />
          <InputField label="Insurance" name="insurance" type="number" register={register} options={{ valueAsNumber: true }} />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <InputField label="Start Date" name="startDate" type="date" register={register} />
            <InputField label="End Date" name="endDate" type="date" register={register} />
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
/* export default function CalculatorForm({onSubmit}){
    // state handler for the form
    const {handleSubmit, register} = useForm<CalculateFields>();

    return (
        <>
        <form onSubmit={handleSubmit(onSubmit)}>
            <div className="container">
                <h2 id="calculator-title">Calculator</h2>
                <div className="container-row">
                    <div className="child">
                        <InputField label="Product Description" name="productDescription" type="" register={register} />
                        <InputField label="Country of Origin" name="exporter" type="" register={register} />
                        <InputField label="Product Value (USD)" name="goods_value" type="number" register={register} />
                        <InputField label="Agreement" name="agreement" type="" register={register} />
                        <InputField label="Product Quantity" name="quantity" type="number" register={register} />
                    </div>
                    <div className="child">
                        <InputField label="HS Code" name="hsCode" type="" register={register} />
                        <InputField label="Destination Country" name="importer" type="" register={register} />
                        <InputField label="Shipping Cost (USD)" name="freight" type="number" register={register} />
                        <InputField label="Insurance" name="insurance" type="number" register={register} />
                        <div className="container-row">
                            <InputField label="Start Date" name="start_date" type="date" register={register} />
                            <InputField label="End Date" name="end_date" type="date" register={register} />
                        </div>
                    </div>
                </div>
                <button id="calculate-button" type="submit">Calculate Landed Cost</button>
            </div>
        </form>
        </>

    )
} */