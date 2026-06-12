import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { getAdminShiftChangeRequests, respondToAdminShiftChangeRequest } from "../adminShiftChangeRequestService";
import { Loader2 } from "lucide-react";
import { Tag } from "~/components/atoms";
import { Modal } from "~/components/molecules/Modal";
import { Eye } from "lucide-react";

const statusColors: Record<string, "default" | "green" | "blue" | "amber" | "red"> = {
    PENDING: "amber",
    ACCEPTED: "green",
    REJECTED: "red",
    CANCELLED: "default",
};

const statusLabels: Record<string, string> = {
    PENDING: "Chờ duyệt",
    ACCEPTED: "Đã chấp nhận",
    REJECTED: "Đã từ chối",
    CANCELLED: "Đã huỷ",
};

export function ShiftChangeRequestPage() {
    const [page, setPage] = useState(0);
    const [selectedRequest, setSelectedRequest] = useState<any | null>(null);
    const queryClient = useQueryClient();

    const { data, isLoading } = useQuery({
        queryKey: ["adminShiftChangeRequests", page],
        queryFn: () => getAdminShiftChangeRequests({ page, size: 20 }),
    });

    const respondMutation = useMutation({
        mutationFn: ({ id, action }: { id: string; action: "ACCEPTED" | "REJECTED" }) =>
            respondToAdminShiftChangeRequest(id, action),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["adminShiftChangeRequests"] });
        },
    });

    if (isLoading) {
        return (
            <div data-testid="loading-spinner" className="flex h-64 items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-6">
            <h1 className="text-2xl font-semibold text-slate-800">Quản lý duyệt đổi ca</h1>
            
            <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm text-slate-600">
                        <thead className="bg-slate-50 text-slate-800">
                            <tr>
                                <th className="px-6 py-4 font-semibold">Ngày làm việc</th>
                                <th className="px-6 py-4 font-semibold">Ca làm việc</th>
                                <th className="px-6 py-4 font-semibold">Nhân viên xin đổi</th>
                                <th className="px-6 py-4 font-semibold">Người thay thế</th>
                                <th className="px-6 py-4 font-semibold">Lý do</th>
                                <th className="px-6 py-4 font-semibold">Trạng thái</th>
                                <th className="px-6 py-4 font-semibold text-right">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {data?.content.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="px-6 py-8 text-center text-slate-500">
                                        Không có yêu cầu đổi ca nào.
                                    </td>
                                </tr>
                            ) : (
                                data?.content.map((request) => (
                                    <tr key={request.id} className="hover:bg-slate-50/50">
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            {request.workDate
                                                ? new Date(request.workDate).toLocaleDateString("vi-VN")
                                                : "N/A"}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">{request.shiftName}</td>
                                        <td className="px-6 py-4 whitespace-nowrap font-medium text-slate-900">
                                            {request.requestedBy}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            {request.targetStaff || <span className="text-slate-400 italic">Tự điều phối</span>}
                                        </td>
                                        <td className="px-6 py-4">{request.reason}</td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <Tag tone={statusColors[request.statusCode]}>
                                                {statusLabels[request.statusCode] || request.statusCode}
                                            </Tag>
                                        </td>
                                        <td className="px-6 py-4 text-right whitespace-nowrap">
                                            {request.statusCode === "PENDING" && (
                                                <div className="flex justify-end gap-2">
                                                    <button
                                                        onClick={() =>
                                                            respondMutation.mutate({ id: request.id, action: "ACCEPTED" })
                                                        }
                                                        disabled={respondMutation.isPending}
                                                        className="rounded-lg bg-emerald-50 px-3 py-1.5 text-sm font-medium text-emerald-600 transition-colors hover:bg-emerald-100 disabled:opacity-50"
                                                    >
                                                        Duyệt
                                                    </button>
                                                    <button
                                                        onClick={() =>
                                                            respondMutation.mutate({ id: request.id, action: "REJECTED" })
                                                        }
                                                        disabled={respondMutation.isPending}
                                                        className="rounded-lg bg-red-50 px-3 py-1.5 text-sm font-medium text-red-600 transition-colors hover:bg-red-100 disabled:opacity-50"
                                                    >
                                                        Từ chối
                                                    </button>
                                                </div>
                                            )}
                                            {request.statusCode !== "PENDING" && (
                                                <button
                                                    onClick={() => setSelectedRequest(request)}
                                                    className="inline-flex items-center gap-1 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 px-3 py-1.5 transition-colors"
                                                    aria-label="Xem chi tiết"
                                                >
                                                    <Eye className="h-4 w-4" />
                                                    <span className="text-sm font-medium">Chi tiết</span>
                                                </button>
                                            )}
                                            {request.statusCode === "PENDING" && (
                                                <button
                                                    onClick={() => setSelectedRequest(request)}
                                                    className="inline-flex items-center justify-center p-1.5 text-slate-400 hover:text-slate-600 transition-colors ml-2"
                                                    aria-label="Xem chi tiết"
                                                    title="Xem chi tiết"
                                                >
                                                    <Eye className="h-5 w-5" />
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
            
            {/* Phân trang đơn giản */}
            {data && data.totalPages > 1 && (
                <div className="flex items-center justify-end gap-2">
                    <button
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                        disabled={page === 0}
                        className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium disabled:opacity-50"
                    >
                        Trước
                    </button>
                    <span className="text-sm text-slate-600">
                        Trang {page + 1} / {data.totalPages}
                    </span>
                    <button
                        onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                        disabled={page >= data.totalPages - 1}
                        className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium disabled:opacity-50"
                    >
                        Sau
                    </button>
                </div>
            )}

            <Modal
                isOpen={!!selectedRequest}
                onClose={() => setSelectedRequest(null)}
                title="Chi tiết yêu cầu đổi ca"
            >
                {selectedRequest && (
                    <div className="space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <p className="text-sm text-slate-500">Người gửi yêu cầu</p>
                                <p className="font-medium text-slate-900">{selectedRequest.requestedBy}</p>
                            </div>
                            <div>
                                <p className="text-sm text-slate-500">Người thay thế (đích)</p>
                                <p className="font-medium text-slate-900">
                                    {selectedRequest.targetStaff || "Tự điều phối (Bất kỳ ai cùng chuyên môn)"}
                                </p>
                            </div>
                            <div>
                                <p className="text-sm text-slate-500">Ngày làm việc</p>
                                <p className="font-medium text-slate-900">
                                    {selectedRequest.workDate
                                        ? new Date(selectedRequest.workDate).toLocaleDateString("vi-VN")
                                        : "N/A"}
                                </p>
                            </div>
                            <div>
                                <p className="text-sm text-slate-500">Ca làm việc</p>
                                <p className="font-medium text-slate-900">{selectedRequest.shiftName}</p>
                            </div>
                            <div className="col-span-2">
                                <p className="text-sm text-slate-500">Trạng thái hiện tại</p>
                                <div className="mt-1">
                                    <Tag tone={statusColors[selectedRequest.statusCode]}>
                                        {statusLabels[selectedRequest.statusCode] || selectedRequest.statusCode}
                                    </Tag>
                                </div>
                            </div>
                            <div className="col-span-2 rounded-lg bg-slate-50 p-4 border border-slate-100">
                                <p className="text-sm text-slate-500 mb-1">Lý do đổi ca</p>
                                <p className="text-slate-700">{selectedRequest.reason}</p>
                            </div>
                        </div>
                        
                        {selectedRequest.statusCode === "PENDING" && (
                            <div className="pt-4 mt-4 border-t border-slate-100 flex justify-end gap-3">
                                <button
                                    onClick={() => {
                                        respondMutation.mutate({ id: selectedRequest.id, action: "REJECTED" });
                                        setSelectedRequest(null);
                                    }}
                                    disabled={respondMutation.isPending}
                                    className="rounded-lg bg-white border border-red-200 px-4 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50 disabled:opacity-50"
                                >
                                    Từ chối yêu cầu
                                </button>
                                <button
                                    onClick={() => {
                                        respondMutation.mutate({ id: selectedRequest.id, action: "ACCEPTED" });
                                        setSelectedRequest(null);
                                    }}
                                    disabled={respondMutation.isPending}
                                    className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-700 disabled:opacity-50"
                                >
                                    Phê duyệt yêu cầu
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </Modal>
        </div>
    );
}
