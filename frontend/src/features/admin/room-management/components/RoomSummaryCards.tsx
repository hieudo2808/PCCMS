import { Card, SummaryRow } from "~/components/molecules";

interface RoomSummaryCardsProps {
    total: number;
    empty: number;
    occupied: number;
    maintenance: number;
}

export function RoomSummaryCards({ total, empty, occupied, maintenance }: RoomSummaryCardsProps) {
    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Card>
                <SummaryRow label="Tổng số phòng" value={String(total)} />
            </Card>
            <Card>
                <SummaryRow label="Phòng trống" value={String(empty)} />
            </Card>
            <Card>
                <SummaryRow label="Đang sử dụng" value={String(occupied)} />
            </Card>
            <Card>
                <SummaryRow label="Bảo trì" value={String(maintenance)} />
            </Card>
        </div>
    );
}
