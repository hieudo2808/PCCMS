import { AlertTriangle, CheckCircle } from "lucide-react";

interface AlertCardProps {
    pet: string;
    metric: string;
    note: string;
    type?: "warning" | "success";
}

export function AlertCard({ pet, metric, note, type = "warning" }: AlertCardProps) {
    const isWarning = type === "warning";
    
    return (
        <div 
            className={`rounded-3xl border p-5 transition-all duration-200 hover:shadow-md ${
                isWarning 
                    ? "border-rose-200 bg-rose-50" 
                    : "border-emerald-200 bg-emerald-50"
            }`}
        >
            <div 
                className={`flex items-center gap-2 ${
                    isWarning ? "text-rose-700" : "text-emerald-700"
                }`}
            >
                {isWarning ? (
                    <AlertTriangle className="h-4 w-4" />
                ) : (
                    <CheckCircle className="h-4 w-4" />
                )}
                <span className="text-sm font-medium">{pet}</span>
            </div>
            <p 
                className={`mt-3 text-lg font-semibold ${
                    isWarning ? "text-rose-900" : "text-emerald-900"
                }`}
            >
                {metric}
            </p>
            <p 
                className={`mt-1 text-sm ${
                    isWarning ? "text-rose-800" : "text-emerald-800"
                }`}
            >
                {note}
            </p>
        </div>
    );
}
