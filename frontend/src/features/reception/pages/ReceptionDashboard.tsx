import { Building2, CalendarDays, Users, Sparkles } from "lucide-react";
import { Tag } from "~/components/atoms";
import { Card, DataTable, MiniGridStats } from "~/components/molecules";
import { SkeletonLoader } from "~/shared/components/SkeletonLoader";
import { useQuery } from "@tanstack/react-query";
import { appointmentApi } from "~/shared/api/appointmentApi";
import { groomingApi } from "~/features/grooming/api/groomingApi";
import { boardingApi } from "~/features/boarding/api/boardingApi";

function formatTime(isoString: string): string {
    const d = new Date(isoString);
    return d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
}

const statusTag = (status: string) => {
    switch (status) {
        case "REQUESTED":
        case "PENDING":
            return <Tag tone="amber">Chờ tiếp nhận</Tag>;
        case "CONFIRMED":
            return <Tag tone="blue">Đã xác nhận</Tag>;
        case "IN_PROGRESS":
        case "IN_SERVICE":
            return <Tag tone="blue">Đang phục vụ</Tag>;
        case "CHECKED_IN":
            return <Tag tone="green">Đã check-in</Tag>;
        case "COMPLETED":
            return <Tag tone="green">Hoàn tất</Tag>;
        case "CANCELLED":
            return <Tag tone="red">Đã hủy</Tag>;
        default:
            return <Tag tone="default">{status}</Tag>;
    }
};

export function ReceptionDashboard() {
    const { data: todayAppointments, isLoading: apptsLoading } = useQuery({
        queryKey: ["reception-today-appointments"],
        queryFn: () => appointmentApi.listTodayAppointments(),
    });

    const { data: groomingData, isLoading: groomingLoading } = useQuery({
        queryKey: ["reception-grooming"],
        queryFn: () => groomingApi.getTickets(),
    });

    const { data: boardingData, isLoading: boardingLoading } = useQuery({
        queryKey: ["reception-boarding"],
        queryFn: () => boardingApi.getBookings(),
    });

    const isLoading = apptsLoading && groomingLoading && boardingLoading;

    if (isLoading) {
        return (
            <div className="space-y-6">
                <SkeletonLoader className="h-28 w-full rounded-2xl" />
                <SkeletonLoader className="h-64 w-full rounded-2xl" />
            </div>
        );
    }

    const appointments = todayAppointments ?? [];
    const pendingCount = appointments.filter(
        (a: any) => a.statusCode === "REQUESTED" || a.statusCode === "PENDING"
    ).length;

    const groomingContent = groomingData?.content ?? [];
    const inServiceGrooming = groomingContent.filter(
        (t: any) => t.statusCode === "IN_SERVICE"
    ).length;

    const boardingContent = boardingData?.content ?? [];
    const activeBoarding = boardingContent.filter(
        (b: any) => b.statusCode === "IN_STAY" || b.statusCode === "CHECKED_IN"
    ).length;

    const pendingCustomers = pendingCount; // customers waiting at counter

    const tableRows = appointments
        .slice(0, 10)
        .map((a: any) => [
            formatTime(a.scheduledStartAt),
            a.ownerName ?? "-",
            a.petName ?? "-",
            a.appointmentType === "MEDICAL" ? "Khám bệnh" : "Spa",
            statusTag(a.statusCode),
            "Tiếp nhận",
        ]);

    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    {
                        label: "Lịch hẹn hôm nay",
                        value: String(appointments.length),
                        hint: pendingCount > 0 ? `${pendingCount} lịch chưa tiếp nhận` : undefined,
                        icon: CalendarDays,
                    },
                    {
                        label: "Spa trong ngày",
                        value: String(groomingContent.length),
                        hint:
                            inServiceGrooming > 0
                                ? `${inServiceGrooming} thú cưng đang phục vụ`
                                : undefined,
                        icon: Sparkles,
                    },
                    {
                        label: "Đang lưu trú",
                        value: String(activeBoarding),
                        hint: undefined,
                        icon: Building2,
                    },
                    {
                        label: "Khách chờ tại quầy",
                        value: String(pendingCustomers),
                        hint: pendingCustomers > 0 ? "Cần tạo nhanh" : undefined,
                        icon: Users,
                    },
                ]}
            />
            <Card
                title="Bảng điều phối nhanh"
                subtitle="Tổng hợp nhanh các ca đang có trong các màn hình nghiệp vụ của nhân viên trung tâm."
            >
                {tableRows.length === 0 ? (
                    <p className="text-sm text-slate-400 py-8 text-center">
                        Không có lịch hẹn nào hôm nay.
                    </p>
                ) : (
                    <DataTable
                        columns={[
                            "Khung giờ",
                            "Khách",
                            "Thú cưng",
                            "Dịch vụ",
                            "Trạng thái",
                            "Hành động",
                        ]}
                        rows={tableRows}
                    />
                )}
            </Card>
        </div>
    );
}
