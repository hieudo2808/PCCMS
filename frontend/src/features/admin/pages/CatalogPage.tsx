import { Tag } from "~/components/atoms";
import { Card, DataTable } from "~/components/molecules";

export function CatalogPage() {
    return (
        <div className="grid gap-6 xl:grid-cols-2">
            <Card title="Danh mục thuốc">
                <DataTable
                    columns={["Tên thuốc", "Tồn kho", "Đơn vị", "Hướng dẫn", "Hành động"]}
                    rows={[
                        ["Amoxicillin", "124", "Hộp", "Ngày 2 lần sau ăn", "Sửa"],
                        ["Metronidazole", "58", "Hộp", "Theo chỉ định bác sĩ", "Sửa"],
                    ]}
                />
            </Card>
            <Card title="Danh mục dịch vụ">
                <DataTable
                    columns={["Dịch vụ", "Nhóm", "Giá", "Trạng thái", "Hành động"]}
                    rows={[
                        [
                            "Tắm + Sấy + Cắt tỉa",
                            "Spa",
                            "350.000đ",
                            <Tag tone="green">Đang mở</Tag>,
                            "Sửa",
                        ],
                        [
                            "Khám tổng quát",
                            "Khám bệnh",
                            "250.000đ",
                            <Tag tone="green">Đang mở</Tag>,
                            "Sửa",
                        ],
                    ]}
                />
            </Card>
        </div>
    );
}
