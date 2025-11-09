type InputProps = {
  id: string;
  label: string;
  type?: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
};

export default function Input({
  id, label, type = "text", value, onChange, placeholder,
}: InputProps) {
  return (
    <label htmlFor={id} className="block w-full">
      <span className="block mb-1 text-sm font-medium text-gray-700">{label}</span>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full px-5 py-2.5 border border-gray-300 rounded-lg
                   focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                   outline-none text-gray-800 transition-all duration-150 sm:text-sm"
        required
      />
    </label>
  );
}