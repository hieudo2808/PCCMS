import { Card } from "~/components/molecules";

interface MedicineSummaryCardsProps {
    total: number;
    inStock: number;
    lowStock: number;
    groups: number;
}

export function MedicineSummaryCards({ total, inStock, lowStock, groups }: MedicineSummaryCardsProps) {
    const cards = [
        { label: "Tổng số thuốc", value: total },
        { label: "Thuốc còn hàng", value: inStock },
        { label: "Thuốc sắp hết", value: lowStock },
        { label: "Nhóm thuốc", value: groups },
    ];

    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {cards.map((item) => (
                <Card key={item.label} className="shadow-sm">
                    <p className="text-sm text-slate-500">{item.label}</p>
                    <p className="mt-2 text-3xl font-bold text-slate-900">{item.value}</p>
                </Card>
            ))}
        </div>
    );
}
