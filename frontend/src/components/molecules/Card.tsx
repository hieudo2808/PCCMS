import type { ReactNode } from "react";
import { cx } from "~/utils/cx";

interface CardProps {
    title?: string;
    subtitle?: string;
    children: ReactNode;
    right?: ReactNode;
    className?: string;
}

export function Card({ title, subtitle, children, right, className }: CardProps) {
    return (
        <div
            className={cx("rounded-3xl border border-slate-200 bg-white p-5 shadow-sm", className)}
        >
            {(title || subtitle || right) && (
                <div className="mb-4 flex items-start justify-between gap-3">
                    <div>
                        {title && <h3 className="text-base font-semibold">{title}</h3>}
                        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
                    </div>
                    {right}
                </div>
            )}
            {children}
        </div>
    );
}
