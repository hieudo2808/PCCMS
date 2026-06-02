import { Search } from "lucide-react";
import { Button, Input, Select, Tag } from "~/components/atoms";
import { Card, DataTable } from "~/components/molecules";

export function AppointmentReceptionPage() {
    return (
        <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
            <Card title="Danh sách lịch hẹn hôm nay">
                <div className="mb-4 grid gap-3 md:grid-cols-[1fr_180px_180px]">
                    <div className="relative">
                        <Search className="pointer-events-none absolute left-4 top-3.5 h-4 w-4 text-slate-400" />
                        <input
                            className="w-full rounded-2xl border border-slate-300 bg-white px-11 py-3 text-sm outline-none transition focus:border-emerald-500"
                            placeholder="Tìm theo SĐT hoặc tên khách"
                        />
                    </div>
                    <Select
                        label=""
                        options={["Tất cả trạng thái", "Chờ tiếp nhận", "Đang chờ khám", "Đã hủy"]}
                    />
                    <Button variant="outline" className="py-3">
                        Tạo nhanh lịch hẹn
                    </Button>
                </div>
                <DataTable
                    columns={[
                        "Mã lịch",
                        "Giờ hẹn",
                        "Khách",
                        "Thú cưng",
                        "Bác sĩ",
                        "Trạng thái",
                        "Hành động",
                    ]}
                    rows={[
                        [
                            "AP0001",
                            "09:00",
                            "Nguyễn Minh",
                            "Milu",
                            "BS An",
                            <Tag tone="amber">Chờ tiếp nhận</Tag>,
                            "Tiếp nhận / Hủy",
                        ],
                        [
                            "AP0002",
                            "09:30",
                            "Lê Hà",
                            "Mít",
                            "BS Hương",
                            <Tag tone="blue">Đang chờ khám</Tag>,
                            "Xem",
                        ],
                        [
                            "AP0003",
                            "10:00",
                            "Hoàng Lan",
                            "Bơ",
                            "BS An",
                            <Tag tone="red">Đã hủy</Tag>,
                            "Xem",
                        ],
                    ]}
                />
            </Card>
            <Card title="Tạo nhanh tại quầy" subtitle="Dành cho khách walk-in">
                <div className="space-y-4">
                    <Input label="Số điện thoại" placeholder="0912 345 678" />
                    <Select label="Bác sĩ" options={["Hệ thống tự gán", "BS An", "BS Hương"]} />
                    <Select label="Thú cưng" options={["Chọn sau khi tra cứu", "Milu", "Bơ"]} />
                    <Button className="w-full py-3">Tiếp nhận ngay</Button>
                    <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
                        Khi tiếp nhận thành công, hệ thống sẽ tự động đẩy thú cưng sang danh sách
                        chờ khám của bác sĩ.
                    </div>
                </div>
            </Card>
        </div>
    );
}
