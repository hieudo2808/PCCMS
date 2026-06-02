import { Users, Scissors, Warehouse, CreditCard } from "lucide-react";
import { Card, MiniGridStats, Notice } from "~/components/molecules";

export function AdminDashboard() {
    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    {
                        label: "Tài khoản hoạt động",
                        value: "246",
                        hint: "12 tài khoản mới tháng này",
                        icon: Users,
                    },
                    {
                        label: "Dịch vụ đang mở",
                        value: "18",
                        hint: "3 gói premium",
                        icon: Scissors,
                    },
                    {
                        label: "Chuồng khả dụng",
                        value: "27/40",
                        hint: "Tỷ lệ lấp đầy 32%",
                        icon: Warehouse,
                    },
                    {
                        label: "Doanh thu tháng",
                        value: "84M",
                        hint: "+12% so với tháng trước",
                        icon: CreditCard,
                    },
                ]}
            />
            <Card title="Cần chú ý">
                <div className="grid gap-4 lg:grid-cols-3">
                    <Notice
                        tone="amber"
                        title="3 ca chưa có lịch trực"
                        text="Cần bổ sung nhân sự khung 16:00 - 18:00 cuối tuần."
                    />
                    <Notice
                        tone="red"
                        title="2 tài khoản bị khóa"
                        text="Kiểm tra nguyên nhân và lịch sử thao tác trước khi mở lại."
                    />
                    <Notice
                        tone="green"
                        title="Dịch vụ spa mới hoạt động tốt"
                        text="Tỷ lệ đặt lịch tuần đầu đạt 78%."
                    />
                </div>
            </Card>
        </div>
    );
}
