import { Stethoscope, CheckCircle2, FileText, Pill } from "lucide-react";
import { Card, MiniGridStats, AlertCard } from "~/components/molecules";

export function DoctorDashboard() {
    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    { label: "Đang chờ khám", value: "7", hint: "3 ca ưu tiên", icon: Stethoscope },
                    {
                        label: "Đã khám hôm nay",
                        value: "11",
                        hint: "2 ca tái khám",
                        icon: CheckCircle2,
                    },
                    {
                        label: "Bệnh án nháp",
                        value: "3",
                        hint: "Cần hoàn tất trước cuối ca",
                        icon: FileText,
                    },
                    { label: "Đơn thuốc đã kê", value: "9", hint: "Kho còn đủ tồn", icon: Pill },
                ]}
            />
            <Card title="Cảnh báo sinh hiệu">
                <div className="grid gap-4 lg:grid-cols-3">
                    <AlertCard pet="Milu" metric="Nhiệt độ 39.4°C" note="Vượt ngưỡng ổn định" />
                    <AlertCard pet="Bơ" metric="SpO2 93%" note="Cần theo dõi thêm" />
                    <AlertCard pet="Luna" metric="CRT 2.4 giây" note="Dấu hiệu tuần hoàn chậm" />
                </div>
            </Card>
        </div>
    );
}
