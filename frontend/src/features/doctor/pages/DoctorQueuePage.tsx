import { Tag } from "~/components/atoms";
import { Card, DataTable } from "~/components/molecules";

export function DoctorQueuePage() {
    return (
        <div className="space-y-6">
            <Card title="Danh sách chờ khám">
                <DataTable
                    columns={[
                        "Thứ tự",
                        "Thú cưng",
                        "Chủ nuôi",
                        "Giờ nhận",
                        "Triệu chứng ban đầu",
                        "Mức ưu tiên",
                        "Hành động",
                    ]}
                    rows={[
                        [
                            1,
                            "Milu",
                            "Nguyễn Minh",
                            "09:05",
                            "Nôn, bỏ ăn 2 ngày",
                            <Tag tone="red">Cao</Tag>,
                            "Mở bệnh án",
                        ],
                        [
                            2,
                            "Bơ",
                            "Hoàng Lan",
                            "09:17",
                            "Tái khám da liễu",
                            <Tag tone="amber">Trung bình</Tag>,
                            "Mở bệnh án",
                        ],
                        [
                            3,
                            "Mít",
                            "Lê Hà",
                            "09:30",
                            "Khám định kỳ",
                            <Tag tone="green">Thấp</Tag>,
                            "Mở bệnh án",
                        ],
                    ]}
                />
            </Card>
        </div>
    );
}
