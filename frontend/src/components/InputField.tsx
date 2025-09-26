import React from "react";
import type { UseFormRegister, FieldValues, Path, RegisterOptions } from "react-hook-form";

type MyInputProps<TForm extends FieldValues> = {
  label: string;
  name: Path<TForm>;
  type?: string;
  register: UseFormRegister<TForm>;
  options?: RegisterOptions<TForm, Path<TForm>>;
  className?: string;
};

export default function InputField<TForm extends FieldValues>({
  label,
  name,
  type = "text",
  register,
  options,
  className,
}: MyInputProps<TForm>) {
  return (
    <div className={`space-y-1 ${className ?? ""}`}>
      <label
        htmlFor={name}
        className="block text-sm font-semibold text-gray-700"
      >
        {label}
      </label>
      <input
        id={name}
        type={type}
        {...register(name, options)}
        className="w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
    </div>
  );
}