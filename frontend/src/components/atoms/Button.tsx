import type { ReactNode, ButtonHTMLAttributes } from "react";
import { cx } from "~/utils/cx";

const variantStyles: Record<string, string> = {
    primary: "bg-emerald-600 text-white hover:bg-emerald-700",
    secondary: "bg-slate-900 text-white hover:bg-slate-800",
    ghost: "bg-slate-100 text-slate-700 hover:bg-slate-200",
    soft: "bg-amber-50 text-amber-800 hover:bg-amber-100",
    outline: "border border-slate-300 bg-white text-slate-700 hover:bg-slate-50",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    children: ReactNode;
    variant?: "primary" | "secondary" | "ghost" | "soft" | "outline";
}

export function Button({
    children,
    variant = "primary",
    className = "",
    type = "button",
    disabled = false,
    ...rest
}: ButtonProps) {
    return (
        <button
            type={type}
            disabled={disabled}
            className={cx(
                "rounded-2xl px-4 py-2 text-sm font-medium transition",
                variantStyles[variant],
                disabled && "cursor-not-allowed opacity-50",
                className
            )}
            {...rest}
        >
            {children}
        </button>
    );
}
