import { Button, Select, Textarea } from "~/components/atoms";
import { Card } from "~/components/molecules";

export function BoardingLogPage() {
    return (
        <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
            <Card title="Thú cưng đang lưu trú">
                <div className="space-y-3">
                    {([
                        ["C12 - Milu", "Ngày 2/3", "Đã cập nhật sáng"],
                        ["B03 - Bơ", "Ngày 1/2", "Chưa cập nhật chiều"],
                        ["A08 - Mít", "Ngày 4/5", "Đủ 3 buổi"],
                    ] as const).map(([name, stay, note]) => (
                        <div key={name} className="cursor-pointer rounded-3xl border border-slate-200 p-4 transition hover:border-emerald-300 hover:shadow-sm">
                            <p className="font-semibold">{name}</p>
                            <p className="mt-1 text-sm text-slate-500">{stay}</p>
                            <p className="mt-2 text-xs text-slate-500">{note}</p>
                        </div>
                    ))}
                </div>
            </Card>
            <Card title="Cập nhật nhật ký hôm nay">
                <div className="grid gap-4 md:grid-cols-2">
                    <Select label="Buổi cập nhật" options={["Sáng", "Trưa", "Chiều"]} />
                    <Select label="Tình trạng ăn uống" options={["Ăn tốt", "Ăn ít", "Bỏ ăn"]} />
                    <Select label="Tình trạng vệ sinh" options={["Bình thường", "Theo dõi thêm", "Bất thường"]} />
                    <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 p-4">
                        <p className="text-sm font-medium">Ảnh / video</p>
                        <p className="mt-2 text-xs text-slate-500">Tối đa 5 tệp, mỗi tệp không quá 10MB.</p>
                        <Button variant="outline" className="mt-3">Tải tệp lên</Button>
                    </div>
                </div>
                <div className="mt-4">
                    <Textarea label="Ghi chú" placeholder="Biểu hiện, thói quen, sức khỏe, giờ uống thuốc..." />
                </div>
                <div className="mt-5 flex gap-2">
                    <Button>Lưu nhật ký</Button>
                    <Button variant="outline">Lưu nháp</Button>
                </div>
            </Card>
        </div>
    );
}
