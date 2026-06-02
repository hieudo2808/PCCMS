import { CalendarDays, Sparkles, Building2, Users } from "lucide-react";
import { Tag } from "~/components/atoms";
import { Card, DataTable, MiniGridStats } from "~/components/molecules";

export function ReceptionDashboard() {
    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    {
                        label: "Lịch hẹn hôm nay",
                        value: "24",
                        hint: "5 lịch chưa tiếp nhận",
                        icon: CalendarDays,
                    },
                    {
                        label: "Spa trong ngày",
                        value: "13",
                        hint: "4 thú cưng đang phục vụ",
                        icon: Sparkles,
                    },
                    {
                        label: "Đang lưu trú",
                        value: "18",
                        hint: "3 nhật ký chưa cập nhật",
                        icon: Building2,
                    },
                    {
                        label: "Khách chờ tại quầy",
                        value: "4",
                        hint: "1 ca cần tạo nhanh",
                        icon: Users,
                    },
                ]}
            />
            <Card title="Bảng điều phối nhanh">
                <DataTable
                    columns={[
                        "Khung giờ",
                        "Khách",
                        "Thú cưng",
                        "Dịch vụ",
                        "Trạng thái",
                        "Hành động",
                    ]}
                    rows={[
                        [
                            "09:00",
                            "Nguyễn Minh",
                            "Milu",
                            "Khám bệnh",
                            <Tag tone="amber">Chờ tiếp nhận</Tag>,
                            "Tiếp nhận",
                        ],
                        [
                            "09:30",
                            "Lê Hà",
                            "Mít",
                            "Spa",
                            <Tag tone="blue">Đang phục vụ</Tag>,
                            "Xem",
                        ],
                        [
                            "10:00",
                            "Hoàng Lan",
                            "Bơ",
                            "Lưu trú",
                            <Tag tone="green">Đã check-in</Tag>,
                            "Xem",
                        ],
                    ]}
                />
            </Card>
        </div>
    );
}
