import React, { type FunctionComponent } from "react";
import { input } from "react-dom";
import type { UseFormRegister } from "react-hook-form";

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

interface MyInputProps {
    name: keyof CalculateFields;
    label: string;
    type: string;
    register: UseFormRegister<CalculateFields>;
}

const InputField : React.FC<MyInputProps> = ({name, label, type, register}) => {
    return (
        <div className="input-container">
            <p className="input-label">{label}</p>
            <input type={type} {...register(name)} />
        </div>
    )
}

export default InputField;