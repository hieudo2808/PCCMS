import { Button, Input, Select, Textarea } from "~/components/atoms";
import { Card, DataTable, Vital } from "~/components/molecules";

export function MedicalRecordPage() {
    return (
        <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
            <Card title="Nhập bệnh án">
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                    <Vital label="Nhiệt độ (°C)" value="39.4" abnormal />
                    <Vital label="Nhịp tim" value="145" abnormal />
                    <Vital label="Nhịp thở" value="26" />
                    <Vital label="Cân nặng (kg)" value="4.5" />
                    <Vital label="Huyết áp" value="120/80" />
                    <Vital label="SpO2 (%)" value="98" />
                    <Vital label="Màu niêm mạc" value="Hồng" />
                    <Vital label="CRT (giây)" value="1.5" />
                </div>
                <div className="mt-4 grid gap-4">
                    <Textarea
                        label="Chẩn đoán ban đầu"
                        value="Nghi viêm đường ruột cấp."
                        rows={3}
                    />
                    <Textarea
                        label="Chẩn đoán xác định"
                        value="Theo dõi thêm sau xét nghiệm máu."
                        rows={3}
                    />
                    <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 p-4">
                        <p className="font-medium">Tệp kết quả xét nghiệm</p>
                        <p className="mt-2 text-sm text-slate-500">
                            Hỗ trợ PDF, PNG, JPG. Tối đa 10MB mỗi tệp.
                        </p>
                        <Button variant="outline" className="mt-3">
                            Tải tệp lên
                        </Button>
                    </div>
                </div>
                <div className="mt-6 flex gap-2">
                    <Button>Lưu nháp</Button>
                    <Button variant="secondary">Xác nhận lưu bệnh án</Button>
                </div>
            </Card>

            <div className="space-y-6">
                <Card title="Kê đơn thuốc">
                    <div className="space-y-4">
                        <Select
                            label="Thuốc"
                            options={["Amoxicillin", "Metronidazole", "Vitamin tổng hợp"]}
                        />
                        <div className="grid gap-4 md:grid-cols-2">
                            <Input label="Liều lượng" placeholder="2 viên/lần" />
                            <Input label="Số lượng kê" placeholder="10" />
                        </div>
                        <Textarea label="Hướng dẫn dùng" value="Ngày 2 lần sau ăn" rows={3} />
                        <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
                            Tồn kho hiện tại: <span className="font-semibold">24 hộp</span>
                        </div>
                        <Button className="w-full py-3">Thêm vào đơn thuốc</Button>
                    </div>
                </Card>
                <Card title="Đơn đã kê hôm nay">
                    <DataTable
                        columns={["Thuốc", "Liều lượng", "Số lượng", "Hướng dẫn"]}
                        rows={[["Amoxicillin", "2 viên/lần", "10", "Ngày 2 lần sau ăn"]]}
                    />
                </Card>
            </div>
        </div>
    );
}
