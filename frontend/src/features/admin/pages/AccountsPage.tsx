import { Search } from "lucide-react";
import { Select, Tag } from "~/components/atoms";
import { Card, DataTable } from "~/components/molecules";

export function AccountsPage() {
    return (
        <div className="space-y-6">
            <Card title="Quản lý tài khoản">
                <div className="mb-4 grid gap-3 md:grid-cols-[1fr_200px_200px]">
                    <div className="relative">
                        <Search className="pointer-events-none absolute left-4 top-3.5 h-4 w-4 text-slate-400" />
                        <input
                            className="w-full rounded-2xl border border-slate-300 bg-white px-11 py-3 text-sm outline-none transition focus:border-emerald-500"
                            placeholder="Tìm theo tên, email, SĐT"
                        />
                    </div>
                    <Select
                        label=""
                        options={[
                            "Tất cả vai trò",
                            "Chủ nuôi",
                            "Nhân viên",
                            "Bác sĩ",
                            "Quản trị viên",
                        ]}
                    />
                    <Select label="" options={["Tất cả trạng thái", "Hoạt động", "Bị khóa"]} />
                </div>
                <DataTable
                    columns={[
                        "Người dùng",
                        "Liên hệ",
                        "Vai trò",
                        "Trạng thái",
                        "Phân quyền",
                        "Hành động",
                    ]}
                    rows={[
                        [
                            "Nguyễn Minh Anh",
                            "minhanh@email.com",
                            "Chủ nuôi",
                            <Tag tone="green">Hoạt động</Tag>,
                            "Giữ nguyên",
                            "Khóa / Sửa",
                        ],
                        [
                            "BS. An",
                            "bsan@pccms.vn",
                            "Bác sĩ",
                            <Tag tone="green">Hoạt động</Tag>,
                            "Bác sĩ",
                            "Sửa",
                        ],
                        [
                            "Lễ tân Hà",
                            "lethan@pccms.vn",
                            "Nhân viên",
                            <Tag tone="red">Bị khóa</Tag>,
                            "Nhân viên",
                            "Mở khóa",
                        ],
                    ]}
                />
            </Card>
        </div>
    );
}
