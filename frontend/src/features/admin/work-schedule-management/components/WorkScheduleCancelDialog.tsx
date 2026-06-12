import { Button } from "~/components/atoms";
import { Card } from "~/components/molecules";
import type { WorkSchedule } from "../types";

interface WorkScheduleCancelDialogProps {
    open: boolean;
    schedule: WorkSchedule | null;
    loading: boolean;
    error?: string;
    onClose: () => void;
    onConfirm: () => void;
}

export function WorkScheduleCancelDialog({ open, schedule, loading, error, onClose, onConfirm }: WorkScheduleCancelDialogProps) {
    if (!open || !schedule) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4">
            <div className="w-full max-w-lg">
                <Card title="Xác nhận hủy lịch làm việc" subtitle={`Bạn có chắc chắn muốn hủy lịch làm việc ${schedule.scheduleCode} không?`}>
                    {error && <p className="mb-4 text-sm font-medium text-error-600">{error}</p>}
                    <div className="flex flex-wrap justify-end gap-3">
                        <Button variant="outline" onClick={onClose} disabled={loading}>
                            Hủy
                        </Button>
                        <Button variant="ghost" onClick={onConfirm} disabled={loading} className="text-rose-700 hover:bg-rose-50">
                            {loading ? "Đang hủy..." : "Xác nhận hủy"}
                        </Button>
                    </div>
                </Card>
            </div>
        </div>
    );
}
