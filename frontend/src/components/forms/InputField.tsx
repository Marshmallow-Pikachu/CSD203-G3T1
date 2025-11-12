import type { UseFormRegister, FieldValues, Path, RegisterOptions } from "react-hook-form";

// shared UI classes (keeps alignment consistent)
const LABEL_CLS =
  "block text-sm font-medium text-gray-700";
const CONTROL_CLS =
  "block w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 shadow-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500 transition";

// ---------- Input ----------
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
    <div className={`space-y-1 min-w-0 ${className ?? ""}`}>
      <label htmlFor={name} className={LABEL_CLS}>
        {label}
      </label>
      <input
        id={name}
        type={type}
        {...register(name, options)}
        className={CONTROL_CLS}
      />
    </div>
  );
}

// ---------- Select ----------
type SelectFieldProps<TForm extends FieldValues> = {
  label: string;
  name: Path<TForm>;
  register: UseFormRegister<TForm>;
  choices: { value: string; label: string }[];
  options?: RegisterOptions<TForm, Path<TForm>>;
  className?: string;
};

export function SelectField<TForm extends FieldValues>({
  label,
  name,
  register,
  options,
  className,
  choices,
}: SelectFieldProps<TForm>) {
  const id = String(name);

  return (
    <div className={`space-y-1 min-w-0 ${className ?? ""}`}>
      <label htmlFor={id} className={LABEL_CLS}>
        {label}
      </label>

      <div className="relative w-full">
        <select
          id={id}
          {...register(name, options)}
          className={`${CONTROL_CLS} appearance-none pr-8`}  // extra right padding for chevron
        >
          <option value="">Select {label}</option>
          {choices.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>

        {/* chevron */}
        <svg
          className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400"
          viewBox="0 0 20 20"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <path
            d="M6 8l4 4 4-4"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </div>
    </div>
  );
}