import { AlertCircle } from "lucide-react";
import { Button } from "../ui/Button";

interface ErrorStateProps {
    title?: string;
    message: string;
    onRetry?: () => void;
}

export function ErrorState({ title = "Có lỗi xảy ra", message, onRetry }: ErrorStateProps) {
    return (
        <div className="flex flex-col items-center justify-center p-8 text-center bg-red-50 rounded-lg border border-red-100">
            <div className="flex items-center justify-center w-12 h-12 mb-4 bg-red-100 rounded-full">
                <AlertCircle className="w-6 h-6 text-red-600" />
            </div>
            <h3 className="mb-1 text-sm font-medium text-red-800">{title}</h3>
            <p className="mb-4 text-sm text-red-600">{message}</p>
            {onRetry && (
                <Button variant="secondary" onClick={onRetry}>
                    Thử lại
                </Button>
            )}
        </div>
    );
}
