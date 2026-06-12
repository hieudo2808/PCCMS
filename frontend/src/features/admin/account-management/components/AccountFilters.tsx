import { Search, RotateCcw } from "lucide-react";
import { Button, Input } from "~/components/atoms";
import { Card } from "~/components/molecules";
import type { AccountRole, AccountSearchParams, AccountStatus } from "../types";

interface AccountFiltersProps {
    value: AccountSearchParams;
    onChange: (value: AccountSearchParams) => void;
    onSearch: () => void;
    onReset: () => void;
    loading: boolean;
    error?: string;
}

const roleOptions: Array<{ value: "" | AccountRole; label: string }> = [
    { value: "", label: "Tất cả vai trò" },
    { value: "admin", label: "Quản trị viên" },
    { value: "staff", label: "Nhân viên trung tâm" },
    { value: "doctor", label: "Bác sĩ thú y" },
    { value: "owner", label: "Chủ nuôi thú cưng" },
];

const statusOptions: Array<{ value: "" | AccountStatus; label: string }> = [
    { value: "", label: "Tất cả trạng thái" },
    { value: "active", label: "Đang hoạt động" },
    { value: "locked", label: "Bị khóa" },
    { value: "disabled", label: "Vô hiệu hóa" },
    { value: "unverified", label: "Chưa xác minh" },
];

export function AccountFilters({ value, onChange, onSearch, onReset, loading, error }: AccountFiltersProps) {
    return (
        <Card title="Bộ lọc tìm kiếm" subtitle="Tìm theo họ tên, email, số điện thoại, vai trò và trạng thái">
            <div className="grid gap-4 lg:grid-cols-5">
                <Input
                    label="Họ tên"
                    value={value.fullName ?? ""}
                    onChange={(event) => onChange({ ...value, fullName: event.target.value })}
                    placeholder="Nhập họ tên"
                />
                <Input
                    label="Email"
                    value={value.email ?? ""}
                    onChange={(event) => onChange({ ...value, email: event.target.value })}
                    placeholder="Nhập email"
                />
                <Input
                    label="Số điện thoại"
                    value={value.phone ?? ""}
                    onChange={(event) => onChange({ ...value, phone: event.target.value })}
                    placeholder="Nhập SĐT"
                />
                <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-medium text-slate-700">Vai trò</label>
                    <select
                        className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-[14px] text-slate-900 outline-none transition-all focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                        value={value.role ?? ""}
                        onChange={(event) => onChange({ ...value, role: event.target.value as AccountRole | "" })}
                    >
                        {roleOptions.map((role) => (
                            <option key={role.value || "all-role"} value={role.value}>
                                {role.label}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="flex flex-col gap-1.5">
                    <label className="text-[13px] font-medium text-slate-700">Trạng thái</label>
                    <select
                        className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-[14px] text-slate-900 outline-none transition-all focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                        value={value.status ?? ""}
                        onChange={(event) => onChange({ ...value, status: event.target.value as AccountStatus | "" })}
                    >
                        {statusOptions.map((status) => (
                            <option key={status.value || "all-status"} value={status.value}>
                                {status.label}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {error && <p className="mt-3 text-sm font-medium text-error-600">{error}</p>}

            <div className="mt-5 flex flex-wrap gap-3">
                <Button onClick={onSearch} disabled={loading} className="inline-flex items-center gap-2">
                    <Search className="h-4 w-4" />
                    {loading ? "Đang tìm..." : "Tìm kiếm"}
                </Button>
                <Button variant="outline" onClick={onReset} disabled={loading} className="inline-flex items-center gap-2">
                    <RotateCcw className="h-4 w-4" />
                    Xóa bộ lọc
                </Button>
            </div>
        </Card>
    );
}
