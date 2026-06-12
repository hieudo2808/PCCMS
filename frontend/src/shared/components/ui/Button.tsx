import { forwardRef } from "react";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Loader2 } from "lucide-react";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: "primary" | "secondary" | "ghost" | "danger";
    loading?: boolean;
    icon?: ReactNode;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
    (
        {
            children,
            variant = "primary",
            loading = false,
            icon,
            className = "",
            disabled,
            ...props
        },
        ref
    ) => {
        const baseStyles =
            "inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed";

        const variants = {
            primary: "bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500",
            secondary: "bg-gray-100 text-gray-900 hover:bg-gray-200 focus:ring-gray-500",
            ghost: "bg-transparent text-gray-700 hover:bg-gray-100 focus:ring-gray-500",
            danger: "bg-red-600 text-white hover:bg-red-700 focus:ring-red-500",
        };

        return (
            <button
                ref={ref}
                className={`${baseStyles} ${variants[variant]} ${className}`}
                disabled={disabled || loading}
                {...props}
            >
                {loading && <Loader2 className="w-4 h-4 animate-spin" />}
                {!loading && icon && <span className="w-4 h-4">{icon}</span>}
                {children}
            </button>
        );
    }
);
Button.displayName = "Button";
