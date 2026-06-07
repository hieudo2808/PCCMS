import type { ReactNode } from "react";
import { Button } from "../ui/Button";

interface ConfirmDialogProps {
    isOpen: boolean;
    title: string;
    message: ReactNode;
    confirmText?: string;
    cancelText?: string;
    onConfirm: () => void;
    onCancel: () => void;
    isDestructive?: boolean;
    isLoading?: boolean;
}

export function ConfirmDialog({
    isOpen,
    title,
    message,
    confirmText = "Xác nhận",
    cancelText = "Hủy",
    onConfirm,
    onCancel,
    isDestructive = false,
    isLoading = false,
}: ConfirmDialogProps) {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/50">
            <div className="w-full max-w-md bg-white rounded-xl shadow-xl">
                <div className="p-6">
                    <h3 className="mb-2 text-lg font-semibold text-gray-900">{title}</h3>
                    <div className="mb-6 text-sm text-gray-500">{message}</div>
                    <div className="flex justify-end gap-3">
                        <Button variant="ghost" onClick={onCancel} disabled={isLoading}>
                            {cancelText}
                        </Button>
                        <Button
                            variant={isDestructive ? "danger" : "primary"}
                            onClick={onConfirm}
                            loading={isLoading}
                        >
                            {confirmText}
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
}
