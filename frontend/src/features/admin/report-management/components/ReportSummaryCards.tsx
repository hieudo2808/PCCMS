import { Card, SummaryRow } from "~/components/molecules";
import type { ReportSummary } from "../types";

interface ReportSummaryCardsProps {
    summary: ReportSummary | null;
}

export function ReportSummaryCards({ summary }: ReportSummaryCardsProps) {
    const data = summary ?? {
        reportType: "-",
        periodLabel: "-",
        totalCount: 0,
        totalRevenue: 0,
        totalValueLabel: "0 đ",
    };

    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Card>
                <SummaryRow label="Loại báo cáo" value={data.reportType} />
            </Card>
            <Card>
                <SummaryRow label="Khoảng thời gian" value={data.periodLabel} />
            </Card>
            <Card>
                <SummaryRow label="Tổng số lượt" value={String(data.totalCount)} />
            </Card>
            <Card>
                <SummaryRow label="Tổng doanh thu" value={data.totalValueLabel} />
            </Card>
        </div>
    );
}
