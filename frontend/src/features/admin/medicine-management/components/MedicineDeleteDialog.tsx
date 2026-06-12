import { Button } from "~/components/atoms";
import { Card } from "~/components/molecules";
import type { Medicine } from "../types";

interface MedicineDeleteDialogProps {
    open: boolean;
    medicine: Medicine | null;
    loading: boolean;
    error?: string;
    onClose: () => void;
    onConfirm: () => void;
}

export function MedicineDeleteDialog({ open, medicine, loading, error, onClose, onConfirm }: MedicineDeleteDialogProps) {
    if (!open || !medicine) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4">
            <div className="w-full max-w-lg">
                <Card title="Xác nhận xóa thuốc" subtitle={`Bạn có chắc chắn muốn xóa thuốc ${medicine.name} không?`}>
                    {error && <p className="mb-4 text-sm font-medium text-error-600">{error}</p>}
                    <div className="flex flex-wrap justify-end gap-3">
                        <Button variant="outline" onClick={onClose} disabled={loading}>
                            Hủy
                        </Button>
                        <Button variant="ghost" onClick={onConfirm} disabled={loading} className="text-rose-700 hover:bg-rose-50">
                            {loading ? "Đang xóa..." : "Xóa"}
                        </Button>
                    </div>
                </Card>
            </div>
        </div>
    );
}