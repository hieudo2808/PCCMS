import { Tag } from "~/components/atoms";
import { Card } from "~/components/molecules";

export function RoomsPage() {
    return (
        <div className="space-y-6">
            <Card title="Quản lý phòng lưu trú">
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                    {(
                        [
                            ["C12", "Premium", "Đang có Milu", "blue"],
                            ["A02", "Tiêu chuẩn", "Còn trống", "green"],
                            ["B07", "Tiêu chuẩn", "Bảo trì", "red"],
                            ["VIP1", "Phòng riêng", "Còn trống", "green"],
                        ] as [string, string, string, "blue" | "green" | "red"][]
                    ).map(([room, type, state, tone]) => (
                        <Card key={room} className="h-full transition hover:shadow-md">
                            <div className="flex items-start justify-between">
                                <div>
                                    <p className="text-xl font-semibold">{room}</p>
                                    <p className="mt-1 text-sm text-slate-500">{type}</p>
                                </div>
                                <Tag tone={tone}>{state}</Tag>
                            </div>
                        </Card>
                    ))}
                </div>
            </Card>
        </div>
    );
}
