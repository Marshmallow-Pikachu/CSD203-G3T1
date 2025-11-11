import type { UseFormRegister, FieldValues, Path, RegisterOptions } from "react-hook-form";

// for dropdown list

type SelectFieldProps<TForm extends FieldValues> = {
  label: string;
  name: Path<TForm>;
  type?: string;
  register: UseFormRegister<TForm>;
  options?: RegisterOptions<TForm, Path<TForm>>;
  className?: string;
  choices: {value: string; label: string}[]; // value - what goes to backend, label - what users see
};

export default function SelectField<TForm extends FieldValues>({
  label,
  name,
  register,
  options,
  className,
  choices,
}: SelectFieldProps<TForm>) {
  return (
    <div className={`space-y-1 ${className ?? ""}`}>
        <label
            htmlFor={name}
            className="block text-sm font-semibold text-gray-700"
            >
            {label}
        </label>
        <select 
            id={name}
            {...register(name, options)}
            className="w-full border border-gray-400 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
            <option value="">Select {label}</option>
            {choices.map((choice) => (
                <option key={choice.value} value={choice.value}>
                    {choice.label}
                </option>
            ))}
        </select>
    </div>
  );
}