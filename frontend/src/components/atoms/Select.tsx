import { type ComponentProps, forwardRef } from "react";
import { cx } from "~/utils/cx";
import { ChevronDown } from "lucide-react";

export type SelectOption = string | { value: string; label: string };

export interface SelectProps extends ComponentProps<"select"> {
    label?: string;
    placeholder?: string;
    options: SelectOption[];
    error?: string;
    helperText?: string;
}

function normalizeOption(opt: SelectOption): { value: string; label: string } {
    return typeof opt === "string" ? { value: opt, label: opt } : opt;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
    ({ label, placeholder, options, error, helperText, className, id, ...props }, ref) => {
        const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, "-") : undefined);
        const normalized = options.map(normalizeOption);

        return (
            <div className="flex flex-col gap-1.5">
                {label && (
                    <label htmlFor={inputId} className="text-[13px] font-medium text-slate-700">
                        {label} {props.required && <span className="text-error-500">*</span>}
                    </label>
                )}
                <div className="relative">
                    <select
                        ref={ref}
                        id={inputId}
                        className={cx(
                            "h-10 w-full appearance-none rounded-xl border bg-white px-3 pr-10 text-[14px] text-slate-900 outline-none transition-all focus:bg-white focus:ring-2",
                            error
                                ? "border-error-500 focus:border-error-500 focus:ring-error-500/20"
                                : "border-slate-200 hover:border-slate-300 focus:border-primary-500 focus:ring-primary-500/20",
                            className
                        )}
                        {...props}
                    >
                        {placeholder && (
                            <option value="">
                                {placeholder}
                            </option>
                        )}
                        {normalized.map((opt) => (
                            <option key={opt.value} value={opt.value}>
                                {opt.label}
                            </option>
                        ))}
                    </select>
                    <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                </div>
                {error ? (
                    <p className="text-[12px] font-medium text-error-600">{error}</p>
                ) : helperText ? (
                    <p className="text-[12px] text-slate-500">{helperText}</p>
                ) : null}
            </div>
        );
    }
);

Select.displayName = "Select";
