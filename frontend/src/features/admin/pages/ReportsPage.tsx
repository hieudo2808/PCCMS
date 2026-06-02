import { CreditCard, Stethoscope, Building2, Scissors } from "lucide-react";
import { Tag } from "~/components/atoms";
import { Card, DataTable, MiniGridStats } from "~/components/molecules";

export function ReportsPage() {
    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    {
                        label: "Doanh thu",
                        value: "84M",
                        hint: "30 ngày gần nhất",
                        icon: CreditCard,
                    },
                    {
                        label: "Lịch khám",
                        value: "146",
                        hint: "Tỷ lệ đến khám 91%",
                        icon: Stethoscope,
                    },
                    {
                        label: "Lưu trú",
                        value: "62",
                        hint: "Tỷ lệ lấp đầy TB 68%",
                        icon: Building2,
                    },
                    {
                        label: "Spa",
                        value: "103",
                        hint: "Dịch vụ phổ biến nhất tháng",
                        icon: Scissors,
                    },
                ]}
            />
            <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
                <Card title="Doanh thu theo tháng">
                    <div className="flex h-72 items-end gap-3 rounded-3xl bg-slate-50 p-5">
                        {[35, 48, 42, 66, 58, 74, 69, 84].map((h, i) => (
                            <div
                                key={i}
                                className="flex-1 rounded-t-2xl bg-emerald-500/80 transition-all hover:bg-emerald-600"
                                style={{ height: `${h}%` }}
                            />
                        ))}
                    </div>
                </Card>
                <Card title="Top dịch vụ">
                    <DataTable
                        columns={["Dịch vụ", "Số lượt", "Doanh thu", "Tăng trưởng"]}
                        rows={[
                            ["Spa premium", "34", "17.6M", <Tag tone="green">+18%</Tag>],
                            ["Khám tổng quát", "51", "12.7M", <Tag tone="green">+8%</Tag>],
                            ["Lưu trú premium", "14", "9.1M", <Tag tone="amber">+3%</Tag>],
                        ]}
                    />
                </Card>
            </div>
        </div>
    );
}
