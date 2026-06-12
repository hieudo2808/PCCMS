import { Users, UserCheck, UserX, Shield } from "lucide-react";
import { Card } from "~/components/molecules";

interface AccountSummaryCardsProps {
    total: number;
    active: number;
    locked: number;
    byRole: {
        admin: number;
        staff: number;
        doctor: number;
        owner: number;
    };
}

import type { LucideIcon } from "lucide-react";

function SummaryCard({
    label,
    value,
    hint,
    icon: Icon,
}: {
    label: string;
    value: number;
    hint: string;
    icon: LucideIcon;
}) {
    return (
        <Card className="p-4">
            <div className="flex items-start justify-between gap-3">
                <div>
                    <p className="text-sm text-slate-500">{label}</p>
                    <p className="mt-2 text-2xl font-semibold text-slate-900">{value}</p>
                    <p className="mt-1 text-xs text-slate-500">{hint}</p>
                </div>
                <div className="rounded-2xl bg-emerald-50 p-3 text-emerald-700">
                    <Icon className="h-5 w-5" />
                </div>
            </div>
        </Card>
    );
}

export function AccountSummaryCards({ total, active, locked, byRole }: AccountSummaryCardsProps) {
    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <SummaryCard label="Tổng tài khoản" value={total} hint="Tất cả tài khoản trong hệ thống" icon={Users} />
            <SummaryCard label="Đang hoạt động" value={active} hint="Tài khoản có thể đăng nhập" icon={UserCheck} />
            <SummaryCard label="Bị khóa" value={locked} hint="Tài khoản đang bị hạn chế" icon={UserX} />
            <SummaryCard
                label="Quản trị viên"
                value={byRole.admin}
                hint={`Nhân sự: ${byRole.staff} • Bác sĩ: ${byRole.doctor} • Chủ nuôi: ${byRole.owner}`}
                icon={Shield}
            />
        </div>
    );
}
