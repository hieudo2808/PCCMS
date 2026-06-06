import { Building2, Scissors, Stethoscope } from "lucide-react";
import { cx } from "~/utils/cx";

export type BookingServiceType = "medical" | "grooming" | "boarding";

const OPTIONS: {
    type: BookingServiceType;
    title: string;
    subtitle: string;
    icon: typeof Stethoscope;
    accent: string;
    selectedRing: string;
    iconBg: string;
}[] = [
    {
        type: "medical",
        title: "Khám bệnh",
        subtitle: "Khám tổng quát với bác sĩ thú y",
        icon: Stethoscope,
        accent: "hover:border-emerald-300 hover:bg-emerald-50/60",
        selectedRing: "border-emerald-500 bg-emerald-50 ring-2 ring-emerald-500",
        iconBg: "bg-emerald-100 text-emerald-700",
    },
    {
        type: "grooming",
        title: "Spa / Làm đẹp",
        subtitle: "Tắm gội, cắt tỉa lông tại trung tâm",
        icon: Scissors,
        accent: "hover:border-violet-300 hover:bg-violet-50/60",
        selectedRing: "border-violet-500 bg-violet-50 ring-2 ring-violet-500",
        iconBg: "bg-violet-100 text-violet-700",
    },
    {
        type: "boarding",
        title: "Lưu trú",
        subtitle: "Gửi thú cưng theo ngày tại phòng lưu trú",
        icon: Building2,
        accent: "hover:border-amber-300 hover:bg-amber-50/60",
        selectedRing: "border-amber-500 bg-amber-50 ring-2 ring-amber-500",
        iconBg: "bg-amber-100 text-amber-700",
    },
];

export function bookingServiceLabel(type: BookingServiceType): string {
    return OPTIONS.find((o) => o.type === type)?.title ?? type;
}

interface BookingServicePickerProps {
    value: BookingServiceType;
    onChange: (type: BookingServiceType) => void;
    className?: string;
}

/** Chọn loại dịch vụ — chỉ dùng trên trang Đặt lịch hẹn (bước 1) */
export function BookingServicePicker({ value, onChange, className }: BookingServicePickerProps) {
    return (
        <div className={cx("grid gap-3 md:grid-cols-3", className)}>
            {OPTIONS.map((option) => {
                const Icon = option.icon;
                const isSelected = value === option.type;
                return (
                    <button
                        key={option.type}
                        type="button"
                        onClick={() => onChange(option.type)}
                        className={cx(
                            "flex flex-col items-start gap-3 rounded-2xl border p-4 text-left transition",
                            isSelected ? option.selectedRing : cx("border-slate-200 bg-white", option.accent)
                        )}
                    >
                        <span
                            className={cx(
                                "flex h-11 w-11 items-center justify-center rounded-xl",
                                option.iconBg
                            )}
                        >
                            <Icon className="h-5 w-5" />
                        </span>
                        <span>
                            <span className="block text-sm font-semibold text-slate-900">
                                {option.title}
                            </span>
                            <span className="mt-1 block text-xs leading-relaxed text-slate-500">
                                {option.subtitle}
                            </span>
                        </span>
                    </button>
                );
            })}
        </div>
    );
}
