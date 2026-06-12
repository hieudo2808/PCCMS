import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import toast from "react-hot-toast";
import { ArrowRight, CalendarDays } from "lucide-react";
import { Tag } from "~/components/atoms";
import { Card, DataTable, EmptyState } from "~/components/molecules";
import { appointmentApi } from "~/shared/api/appointmentApi";
import { hasAccessToken } from "~/shared/auth/tokenStorage";
import { useAuth } from "~/features/auth/context/AuthContext";
import type { AppointmentResponse, AppointmentStatus } from "~/types/appointment";

function statusTone(status: AppointmentStatus): "amber" | "blue" | "red" | "green" {
    switch (status) {
        case "PENDING":
            return "amber";
        case "CHECKED_IN":
        case "CONFIRMED":
            return "blue";
        case "CANCELLED":
            return "red";
        default:
            return "green";
    }
}

function formatDateTime(iso: string) {
    return new Date(iso).toLocaleString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function serviceTypeLabel(type?: string) {
    switch (type) {
        case "GROOMING":
            return "Spa / Làm đẹp";
        case "MEDICAL":
        default:
            return "Khám bệnh";
    }
}

export function OwnerAppointmentsPage() {
    const queryClient = useQueryClient();
    const { isAuthenticated, user } = useAuth();
    const canFetch = isAuthenticated && hasAccessToken() && Boolean(user);

    const { data, isLoading } = useQuery({
        queryKey: ["appointments", "owner"],
        queryFn: () => appointmentApi.listOwnerAppointments({ page: 0, size: 50 }),
        enabled: canFetch,
    });

    const { data: boardingBookings = [], isLoading: boardingLoading } = useQuery({
        queryKey: ["appointments", "boarding", "owner"],
        queryFn: () => appointmentApi.listOwnerBoardingBookings(),
        enabled: canFetch,
    });

    const cancelMutation = useMutation({
        mutationFn: appointmentApi.cancel,
        onSuccess: () => {
            toast.success("Hủy lịch thành công");
            queryClient.invalidateQueries({ queryKey: ["appointments"] });
        },
        onError: () => toast.error("Không thể hủy lịch hẹn"),
    });

    const appointments = data?.content ?? [];

    const rows = appointments.map((apt: AppointmentResponse) => {
        const canCancel = apt.statusCode === "PENDING";
        return [
            apt.appointmentCode,
            serviceTypeLabel(apt.appointmentType),
            apt.petName,
            formatDateTime(apt.scheduledStartAt),
            apt.assignedVetName ?? "—",
            <Tag tone={statusTone(apt.statusCode)}>{apt.statusLabel}</Tag>,
            canCancel ? (
                <button
                    type="button"
                    className="text-sm font-medium text-red-600 hover:underline"
                    onClick={() => cancelMutation.mutate(apt.id)}
                >
                    Hủy
                </button>
            ) : (
                <span className="text-sm text-slate-400">—</span>
            ),
        ];
    });

    const boardingRows = boardingBookings.map((b) => [
        b.bookingCode,
        b.petName,
        b.roomTypeName,
        formatDateTime(b.expectedCheckinAt),
        formatDateTime(b.expectedCheckoutAt),
        <Tag tone={b.statusCode === "CANCELLED" ? "red" : "blue"}>{b.statusLabel}</Tag>,
    ]);

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div>
                    <h2 className="text-xl font-semibold tracking-tight text-slate-900">
                        Lịch hẹn của tôi
                    </h2>
                    <p className="mt-1 text-sm text-slate-500">
                        Xem và quản lý lịch đã đặt — khám, spa, lưu trú
                    </p>
                </div>
                <Link
                    to="/owner/book"
                    className="inline-flex items-center gap-1.5 text-sm font-medium text-emerald-700 transition hover:text-emerald-800"
                >
                    Đặt lịch mới
                    <ArrowRight className="h-4 w-4" />
                </Link>
            </div>

            <Card title="Lịch khám & Spa">
                {isLoading ? (
                    <p className="py-8 text-center text-sm text-slate-500">Đang tải...</p>
                ) : appointments.length === 0 ? (
                    <EmptyState
                        title="Chưa có lịch hẹn"
                        description="Vào trang Đặt lịch hẹn để chọn dịch vụ và tạo lịch mới."
                        icon={<CalendarDays className="h-8 w-8" />}
                        action={
                            <Link
                                to="/owner/book"
                                className="inline-flex items-center gap-2 rounded-2xl bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
                            >
                                Đến trang đặt lịch
                                <ArrowRight className="h-4 w-4" />
                            </Link>
                        }
                    />
                ) : (
                    <DataTable
                        columns={[
                            "Mã lịch",
                            "Loại",
                            "Thú cưng",
                            "Thời gian",
                            "Phụ trách",
                            "Trạng thái",
                            "Hành động",
                        ]}
                        rows={rows}
                    />
                )}
            </Card>

            <Card title="Đặt phòng lưu trú">
                {boardingLoading ? (
                    <p className="py-8 text-center text-sm text-slate-500">Đang tải...</p>
                ) : boardingBookings.length === 0 ? (
                    <EmptyState
                        title="Chưa có đặt phòng lưu trú"
                        description="Đặt phòng trong trang Đặt lịch hẹn — chọn mục Lưu trú."
                        icon={<CalendarDays className="h-8 w-8" />}
                        action={
                            <Link
                                to="/owner/book"
                                className="text-sm font-medium text-emerald-600 hover:underline"
                            >
                                Mở trang đặt lịch
                            </Link>
                        }
                    />
                ) : (
                    <DataTable
                        columns={[
                            "Mã đặt phòng",
                            "Thú cưng",
                            "Loại phòng",
                            "Nhận phòng",
                            "Trả phòng",
                            "Trạng thái",
                        ]}
                        rows={boardingRows}
                    />
                )}
            </Card>
        </div>
    );
}
