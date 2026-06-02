import { cx } from "~/utils/cx";

interface VitalProps {
    label: string;
    value: string;
    abnormal?: boolean;
}

export function Vital({ label, value, abnormal = false }: VitalProps) {
    return (
        <div
            className={cx(
                "rounded-3xl border p-4",
                abnormal ? "border-rose-300 bg-rose-50" : "border-slate-200 bg-slate-50"
            )}
        >
            <p className="text-sm text-slate-500">{label}</p>
            <p
                className={cx(
                    "mt-2 text-xl font-semibold",
                    abnormal ? "text-rose-700" : "text-slate-900"
                )}
            >
                {value}
            </p>
        </div>
    );
}
