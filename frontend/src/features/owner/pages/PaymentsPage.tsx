import { CheckCircle2, Clock3, CreditCard, Scissors } from "lucide-react";
import { Tag } from "~/components/atoms";
import { Card, DataTable, MiniGridStats } from "~/components/molecules";

export function PaymentsPage() {
    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    { label: "Đã thanh toán", value: "12", hint: "Tính từ đầu năm", icon: CheckCircle2 },
                    { label: "Chờ thanh toán", value: "1", hint: "Hóa đơn lưu trú Milu", icon: Clock3 },
                    { label: "Tổng chi tiêu", value: "12.8M", hint: "Khám + spa + lưu trú", icon: CreditCard },
                    { label: "Dịch vụ dùng nhiều nhất", value: "Spa", hint: "5 giao dịch", icon: Scissors },
                ]}
            />
            <Card title="Lịch sử hóa đơn">
                <DataTable
                    columns={["Mã HĐ", "Ngày", "Dịch vụ", "Tổng tiền", "Trạng thái", "Chi tiết"]}
                    rows={[
                        ["HD-2026-041", "24/05/2026", "Lưu trú", "1.260.000đ", <Tag tone="amber">Chờ TT</Tag>, "Xem"],
                        ["HD-2026-032", "20/05/2026", "Khám bệnh", "420.000đ", <Tag tone="green">Đã TT</Tag>, "Xem"],
                        ["HD-2026-021", "14/05/2026", "Spa premium", "520.000đ", <Tag tone="green">Đã TT</Tag>, "Xem"],
                    ]}
                />
            </Card>
        </div>
    );
}
