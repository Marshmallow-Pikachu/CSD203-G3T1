import { useMutation } from "@tanstack/react-query";
import InputField from "./InputField";
import { useForm } from "react-hook-form";

interface CalculateFields {
  productDescription: string;
  hsCode: string;
  exporter: string;
  importer: string;
  agreement: string;
  goods_value: number;
  quantity: number;
  freight: number;
  insurance: string;
  start_date: string;
  end_date: string;
};


export default function CalculatorForm({onSubmit}){
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
}