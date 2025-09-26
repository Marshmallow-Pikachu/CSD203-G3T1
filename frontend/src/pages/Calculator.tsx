import InputField from "../components/InputField";
import CalculatorForm from "../components/CalculatorForm";
import { useForm } from "react-hook-form";
import { useQuery } from "@tanstack/react-query";

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

type Calculate = {
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
}

export default function Calculator(){
    const { data } = useQuery({
        queryKey: ['Calculate']
    })
   
    return (
        <>
        <CalculatorForm />
        </>

    )
}