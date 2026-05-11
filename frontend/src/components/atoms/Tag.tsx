import type { ReactNode } from "react";
import { cx } from "~/utils/cx";

const toneStyles: Record<string, string> = {
    default: "bg-slate-100 text-slate-700",
    green: "bg-emerald-50 text-emerald-700",
    blue: "bg-sky-50 text-sky-700",
    amber: "bg-amber-50 text-amber-700",
    red: "bg-rose-50 text-rose-700",
};

interface TagProps {
    children: ReactNode;
    tone?: "default" | "green" | "blue" | "amber" | "red";
}

export function Tag({ children, tone = "default" }: TagProps) {
    return (
        <span className={cx("inline-flex rounded-full px-3 py-1 text-xs font-medium", toneStyles[tone])}>
            {children}
        </span>
    );
}
