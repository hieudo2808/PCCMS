import { Card, SummaryRow } from "~/components/molecules";

interface WorkScheduleSummaryCardsProps {
    total: number;
    assigned: number;
    cancelled: number;
    staffCount: number;
}

export function WorkScheduleSummaryCards({ total, assigned, cancelled, staffCount }: WorkScheduleSummaryCardsProps) {
    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Card>
                <SummaryRow label="Tổng lịch làm việc" value={String(total)} />
            </Card>
            <Card>
                <SummaryRow label="Đã phân công" value={String(assigned)} />
            </Card>
            <Card>
                <SummaryRow label="Đã hủy" value={String(cancelled)} />
            </Card>
            <Card>
                <SummaryRow label="Nhân sự trong lịch" value={String(staffCount)} />
            </Card>
        </div>
    );
}
