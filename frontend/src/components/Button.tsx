import React from "react";

type ButtonProps = {
  type?: "button" | "submit" | "reset";
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
};

export default function Button({type = "button",children,onClick,disabled = false,}: ButtonProps) {
  return (
    <button type={type} onClick={onClick} disabled={disabled}
    className={`px-6 py-2 bg-blue-600 text-white rounded-lg font-semibold
                  hover:bg-blue-700 focus:ring-2 focus:ring-blue-400
                  transition duration-200 ease-in-out
                  ${disabled ? "opacity-50 cursor-not-allowed" : ""}`}>
      {children}
    </button>
  );
}