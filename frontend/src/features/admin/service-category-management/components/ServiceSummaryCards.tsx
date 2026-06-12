import { Card, SummaryRow } from "~/components/molecules";

interface ServiceSummaryCardsProps {
    total: number;
    active: number;
    inactive: number;
    groups: number;
}

export function ServiceSummaryCards({ total, active, inactive, groups }: ServiceSummaryCardsProps) {
    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Card>
                <SummaryRow label="Tổng dịch vụ" value={String(total)} />
            </Card>
            <Card>
                <SummaryRow label="Đang áp dụng" value={String(active)} />
            </Card>
            <Card>
                <SummaryRow label="Ngừng áp dụng" value={String(inactive)} />
            </Card>
            <Card>
                <SummaryRow label="Nhóm dịch vụ" value={String(groups)} />
            </Card>
        </div>
    );
}
