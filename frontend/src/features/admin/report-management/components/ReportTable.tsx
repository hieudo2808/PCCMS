import { Card } from "~/components/molecules";
import type { ReportRecord } from "../types";

interface ReportTableProps {
    items: ReportRecord[];
}

export function ReportTable({ items }: ReportTableProps) {
    return (
        <Card title="Bảng dữ liệu chi tiết" subtitle="Dữ liệu tổng hợp theo ngày hoặc nhóm">
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                    <thead className="bg-slate-50 text-slate-500">
                        <tr>
                            <th className="px-4 py-3 font-medium">Ngày</th>
                            <th className="px-4 py-3 font-medium">Nhóm / Dịch vụ</th>
                            <th className="px-4 py-3 font-medium">Số lượt</th>
                            <th className="px-4 py-3 font-medium">Doanh thu</th>
                            <th className="px-4 py-3 font-medium">Ghi chú</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200 bg-white">
                        {items.map((item) => (
                            <tr key={item.id}>
                                <td className="px-4 py-3">{item.date}</td>
                                <td className="px-4 py-3">
                                    <div className="space-y-1">
                                        <p className="font-medium text-slate-900">{item.group}</p>
                                        <p className="text-xs text-slate-500">{item.serviceName}</p>
                                    </div>
                                </td>
                                <td className="px-4 py-3">{item.count}</td>
                                <td className="px-4 py-3 font-medium">{item.revenue.toLocaleString("vi-VN")} đ</td>
                                <td className="px-4 py-3">{item.note}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </Card>
    );
}
