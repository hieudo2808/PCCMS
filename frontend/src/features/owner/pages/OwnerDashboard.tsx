import { PawPrint, CalendarDays, Building2, CreditCard } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Card, MiniGridStats, Notice } from "~/components/molecules";
import { SkeletonLoader } from "~/shared/components/SkeletonLoader";
import { appointmentApi } from "~/shared/api/appointmentApi";
import { invoiceApi } from "~/shared/api/invoiceApi";
import { boardingApi } from "~/features/boarding/api/boardingApi";
import { petApi } from "~/shared/api/petApi";
import { notificationApi } from "~/shared/api/notificationApi";

export function OwnerDashboard() {
    const navigate = useNavigate();
    const { data: petsData, isLoading: petsLoading } = useQuery({
        queryKey: ["owner-pets"],
        queryFn: () => petApi.getPets({ page: 0, size: 50 }),
    });

    const { data: appointmentsData, isLoading: apptsLoading } = useQuery({
        queryKey: ["owner-appointments"],
        queryFn: () => appointmentApi.listOwnerAppointments({ page: 0, size: 50 }),
    });

    const { data: boardingData, isLoading: boardingLoading } = useQuery({
        queryKey: ["owner-boarding"],
        queryFn: () => boardingApi.getMyBookings(),
    });

    const { data: invoicesData, isLoading: invoicesLoading } = useQuery({
        queryKey: ["owner-invoices"],
        queryFn: () => invoiceApi.listMyInvoices({ page: 0, size: 100 }),
    });

    const { data: notificationsData, isLoading: notifLoading } = useQuery({
        queryKey: ["owner-notifications"],
        queryFn: () => notificationApi.listMyNotifications({ page: 0, size: 5 }),
    });

    const petsCount = petsData?.content?.length ?? 0;
    const activePetsCount = petsData?.content?.filter((p: any) => p.isActive !== false).length ?? 0;

    const now = new Date();
    const upcomingAppointments = (appointmentsData?.content ?? []).filter((a: any) => {
        const scheduled = new Date(a.scheduledStartAt);
        return scheduled >= now && a.appointmentType === "MEDICAL";
    });
    const upcomingCount = upcomingAppointments.length;
    const soonCount = upcomingAppointments.filter((a: any) => {
        const diff = new Date(a.scheduledStartAt).getTime() - now.getTime();
        return diff <= 24 * 60 * 60 * 1000;
    }).length;

    const activeBoarding = (boardingData?.content ?? []).filter(
        (b: any) => b.statusCode === "IN_STAY" || b.statusCode === "CHECKED_IN"
    );
    const boardingCount = activeBoarding.length;

    const unpaidInvoices = (invoicesData?.content ?? []).filter(
        (i: any) => i.statusCode === "UNPAID" || i.statusCode === "OVERDUE"
    );
    const thisMonthInvoices = (invoicesData?.content ?? []).filter((i: any) => {
        const invDate = new Date(i.createdAt ?? i.issuedAt);
        return invDate.getMonth() === now.getMonth() && invDate.getFullYear() === now.getFullYear();
    });
    const thisMonthCount = thisMonthInvoices.length;

    const notifications = notificationsData?.content ?? [];

    if (petsLoading && apptsLoading && boardingLoading && invoicesLoading && notifLoading) {
        return (
            <div className="space-y-6">
                <SkeletonLoader className="h-28 w-full rounded-2xl" />
                <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
                    <SkeletonLoader className="h-48 w-full rounded-2xl" />
                    <SkeletonLoader className="h-48 w-full rounded-2xl" />
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    {
                        label: "Thú cưng của tôi",
                        value: String(petsCount),
                        hint: `${activePetsCount} hồ sơ đang hoạt động`,
                        icon: PawPrint,
                    },
                    {
                        label: "Lịch hẹn sắp tới",
                        value: String(upcomingCount),
                        hint: soonCount > 0 ? `${soonCount} lịch trong 24 giờ` : undefined,
                        icon: CalendarDays,
                    },
                    {
                        label: "Đang lưu trú",
                        value: String(boardingCount),
                        hint:
                            boardingCount > 0
                                ? `${activeBoarding[0]?.roomName ?? "phòng"}`
                                : undefined,
                        icon: Building2,
                    },
                    {
                        label: "Hóa đơn tháng này",
                        value: String(thisMonthCount),
                        hint:
                            unpaidInvoices.length > 0
                                ? `${unpaidInvoices.length} chưa thanh toán`
                                : undefined,
                        icon: CreditCard,
                    },
                ]}
            />
            <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
                <Card title="Đặt dịch vụ nhanh">
                    <div className="grid gap-4 md:grid-cols-1">
                        <div
                            onClick={() => navigate("/owner/grooming/book")}
                            className="cursor-pointer rounded-3xl border border-slate-200 p-6 transition hover:border-primary-300 hover:shadow-md flex items-center gap-4 bg-primary-50"
                        >
                            <div className="inline-flex rounded-2xl bg-white p-4 text-primary-700 shadow-sm">
                                <CalendarDays className="h-6 w-6" />
                            </div>
                            <div>
                                <h4 className="text-lg font-bold text-slate-900">
                                    Đăng ký dịch vụ làm đẹp
                                </h4>
                                <p className="mt-1 text-sm text-slate-500">
                                    Chọn thú cưng, gói tắm sấy/cắt tỉa và giờ hẹn phù hợp.
                                </p>
                            </div>
                        </div>
                    </div>
                </Card>
                <Card title="Thông báo gần đây">
                    {notifications.length === 0 ? (
                        <p className="text-sm text-slate-400">Không có thông báo nào.</p>
                    ) : (
                        <div className="space-y-3">
                            {notifications.slice(0, 3).map((n) => (
                                <Notice
                                    key={n.id}
                                    tone={n.statusCode === "READ" ? "blue" : "green"}
                                    title={n.title}
                                    text={n.body}
                                />
                            ))}
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
}
