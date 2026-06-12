import { Button } from "~/components/atoms";
import { Card } from "~/components/molecules";
import type { BoardingRoom } from "../types";

interface RoomDeleteDialogProps {
    open: boolean;
    room: BoardingRoom | null;
    loading: boolean;
    error?: string;
    onClose: () => void;
    onConfirm: () => void;
}

export function RoomDeleteDialog({ open, room, loading, error, onClose, onConfirm }: RoomDeleteDialogProps) {
    if (!open || !room) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4">
            <div className="w-full max-w-lg">
                <Card title="Xác nhận xóa phòng" subtitle={`Bạn có chắc chắn muốn xóa phòng ${room.name} không?`}>
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