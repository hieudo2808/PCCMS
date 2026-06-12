import { useEffect, useMemo, useState } from "react";
import { CreditCard, Scissors, Users, Warehouse } from "lucide-react";
import { Card, EmptyState, MiniGridStats, Notice } from "~/components/molecules";
import {
    emptyDashboardSummary,
    formatDashboardRevenue,
    getDashboardSummary,
} from "../dashboard/dashboardService";

export function AdminDashboard() {
    const [summary, setSummary] = useState(emptyDashboardSummary());
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let mounted = true;
        setLoading(true);
        setError("");
        void getDashboardSummary()
            .then((data) => {
                if (mounted) {
                    setSummary(data);
                }
            })
            .catch(() => {
                if (mounted) {
                    setSummary(emptyDashboardSummary());
                    setError("Chưa có dữ liệu tổng quan để hiển thị");
                }
            })
            .finally(() => {
                if (mounted) {
                    setLoading(false);
                }
            });

        return () => {
            mounted = false;
        };
    }, []);

    const stats = useMemo(
        () => [
            {
                label: "Tài khoản hoạt động",
                value: loading ? "—" : String(summary.activeAccounts),
                hint: "Có thể đăng nhập hệ thống",
                icon: Users,
            },
            {
                label: "Dịch vụ đang mở",
                value: loading ? "—" : String(summary.activeServices),
                hint: "Đang áp dụng cho khách hàng",
                icon: Scissors,
            },
            {
                label: "Phòng khả dụng",
                value: loading ? "—" : String(summary.availableRooms),
                hint: "Sẵn sàng tiếp nhận lưu trú",
                icon: Warehouse,
            },
            {
                label: "Doanh thu tháng",
                value: loading ? "—" : formatDashboardRevenue(summary.monthlyRevenueVnd),
                hint: loading ? "Đang cập nhật dữ liệu" : `${summary.monthlyInvoiceCount} hóa đơn đã ghi nhận`,
                icon: CreditCard,
            },
        ],
        [loading, summary]
    );

    return (
        <div className="space-y-6">
            <MiniGridStats items={stats} />

            {error ? (
                <EmptyState
                    title="Chưa có dữ liệu tổng quan để hiển thị"
                    description="Dữ liệu sẽ được cập nhật khi hệ thống ghi nhận phát sinh mới."
                />
            ) : null}

            <Card title="Cần chú ý">
                <div className="grid gap-4 lg:grid-cols-3">
                    <Notice
                        tone={summary.activeAccounts > 0 ? "green" : "amber"}
                        title={loading ? "Đang cập nhật tài khoản" : `${summary.activeAccounts} tài khoản hoạt động`}
                        text="Theo dõi khả năng truy cập của nhân sự và khách hàng."
                    />
                    <Notice
                        tone={summary.availableRooms > 0 ? "green" : "amber"}
                        title={loading ? "Đang cập nhật phòng" : `${summary.availableRooms} phòng khả dụng`}
                        text="Sẵn sàng tiếp nhận các nhu cầu lưu trú mới."
                    />
                    <Notice
                        tone={summary.monthlyRevenueVnd > 0 ? "green" : "amber"}
                        title={loading ? "Đang cập nhật doanh thu" : `Doanh thu tháng ${formatDashboardRevenue(summary.monthlyRevenueVnd)}`}
                        text={`${summary.monthlyInvoiceCount} hóa đơn đã ghi nhận trong tháng.`}
                    />
                </div>
            </Card>
        </div>
    );
}
