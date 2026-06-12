import type { IconComponent } from "~/types/navigation";
import { Card } from "./Card";

interface StatProps {
    label: string;
    value: string;
    hint?: string;
    icon: IconComponent;
}

export function Stat({ label, value, hint, icon: Icon }: StatProps) {
    return (
        <Card className="h-full">
            <div className="flex items-start justify-between gap-3">
                <div>
                    <p className="text-sm text-slate-500">{label}</p>
                    <p className="mt-3 text-3xl font-semibold tracking-tight">{value}</p>
                    {hint && <p className="mt-2 text-xs text-slate-500">{hint}</p>}
                </div>
                <div className="rounded-2xl bg-emerald-50 p-3 text-emerald-700">
                    <Icon className="h-5 w-5" />
                </div>
            </div>
        </Card>
    );
}
