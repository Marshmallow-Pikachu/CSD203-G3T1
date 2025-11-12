import React from "react";

type ButtonProps = {
  type?: "button" | "submit" | "reset";
  onClick?: () => void;
  children: React.ReactNode;
  variant?: "primary" | "secondary" | "danger";
  className?: string;
  disabled?: boolean;
  logo?: React.ReactNode;
};

export default function Button({
  type = "button",
  onClick,
  children,
  variant = "primary",
  className = "",
  disabled = false,
  logo,
}: ButtonProps) {
  let variantStyles = "";

  if (variant === "primary") {
    variantStyles =
      "bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500 shadow-sm hover:shadow-md";
  } else if (variant === "secondary") {
    variantStyles =
      "bg-gray-100 text-gray-800 border border-gray-300 hover:bg-gray-200 focus:ring-gray-400";
  } else if (variant === "danger") {
    variantStyles =
      "bg-red-600 text-white hover:bg-red-700 focus:ring-red-500 shadow-sm hover:shadow-md";
  }

  const baseStyles =
    "inline-flex items-center justify-center w-full px-4 py-2.5 text-sm font-medium rounded-md " +
    "transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-offset-1 " +
    "disabled:opacity-60 disabled:cursor-not-allowed";

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`${baseStyles} ${variantStyles} ${className}`}
    >
      {logo && <span className="mr-2">{logo}</span>}
      {children}
    </button>
  );
}