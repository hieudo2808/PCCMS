import { useState } from "react";
import {
    CheckCircle2,
    Clock3,
    CreditCard,
    Scissors,
    BedDouble,
    Stethoscope,
    ChevronRight,
    X,
    Receipt,
    Wallet,
    QrCode,
    Banknote,
} from "lucide-react";
import { Button, Tag } from "~/components/atoms";
import { MiniGridStats, SummaryRow } from "~/components/molecules";
import {
    type ServiceType,
    type PayMethod,
    type Invoice,
    MOCK_INVOICES,
    SERVICE_TONE,
    STATUS_TONE,
    PAY_METHODS as PAY_METHODS_DATA,
} from "../data/payments.mock";

// SERVICE_ICON and PAY_ICONS use JSX — they stay in the component file
const SERVICE_ICON: Record<ServiceType, React.ReactNode> = {
    Spa: <Scissors className="h-4 w-4" />,
    "Lưu trú": <BedDouble className="h-4 w-4" />,
    "Khám bệnh": <Stethoscope className="h-4 w-4" />,
};

const PAY_ICON_MAP: Record<PayMethod, React.ReactNode> = {
    "Tiền mặt": <Banknote className="h-5 w-5" />,
    "Chuyển khoản": <QrCode className="h-5 w-5" />,
    "Thẻ ngân hàng": <CreditCard className="h-5 w-5" />,
};

// Merge icon into PAY_METHODS
const PAY_METHODS = PAY_METHODS_DATA.map((m) => ({ ...m, icon: PAY_ICON_MAP[m.method] }));

function fmt(n: number) {
    return n.toLocaleString("vi-VN") + "₫";
}

// ─── Payment Modal ─────────────────────────────────────────────────────────────
function PaymentModal({
    invoice,
    onClose,
    onSuccess,
}: {
    invoice: Invoice;
    onClose: () => void;
    onSuccess: (method: PayMethod) => void;
}) {
    const [step, setStep] = useState<"select" | "confirm" | "done">("select");
    const [method, setMethod] = useState<PayMethod>("Chuyển khoản");

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
            <div className="w-full max-w-md rounded-3xl bg-white shadow-2xl">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
                    <h3 className="font-semibold text-slate-800">
                        {step === "done" ? "Thanh toán thành công" : `Thanh toán · ${invoice.code}`}
                    </h3>
                    <button
                        onClick={onClose}
                        className="rounded-xl p-1.5 text-slate-400 transition hover:bg-slate-100"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                <div className="p-6">
                    {/* Step: Select method */}
                    {step === "select" && (
                        <div className="space-y-4">
                            <div className="rounded-2xl bg-slate-50 p-4 space-y-2">
                                <SummaryRow label="Dịch vụ" value={invoice.serviceDetail} />
                                <SummaryRow label="Thú cưng" value={invoice.petName} />
                                <div className="border-t border-slate-200 pt-2">
                                    <SummaryRow label="Tổng thanh toán" value={fmt(invoice.amount)} />
                                </div>
                            </div>

                            <p className="text-sm font-medium text-slate-700">Phương thức thanh toán</p>
                            <div className="space-y-2">
                                {PAY_METHODS.map(({ method: m, icon, desc }) => (
                                    <button
                                        key={m}
                                        onClick={() => setMethod(m)}
                                        className={[
                                            "w-full flex items-center gap-3 rounded-2xl border-2 px-4 py-3 text-left transition-all",
                                            method === m
                                                ? "border-violet-500 bg-violet-50"
                                                : "border-slate-200 hover:border-violet-300",
                                        ].join(" ")}
                                    >
                                        <span className={method === m ? "text-violet-600" : "text-slate-400"}>
                                            {icon}
                                        </span>
                                        <div>
                                            <p className="font-medium text-sm text-slate-800">{m}</p>
                                            <p className="text-xs text-slate-500">{desc}</p>
                                        </div>
                                        {method === m && (
                                            <CheckCircle2 className="ml-auto h-4 w-4 text-violet-500" />
                                        )}
                                    </button>
                                ))}
                            </div>

                            {/* QR placeholder for bank transfer */}
                            {method === "Chuyển khoản" && (
                                <div className="rounded-2xl bg-blue-50 border border-blue-100 p-4 text-center">
                                    <div className="mx-auto mb-3 h-32 w-32 rounded-xl bg-white border border-blue-200 flex items-center justify-center">
                                        <QrCode className="h-20 w-20 text-blue-300" />
                                    </div>
                                    <p className="text-sm font-medium text-blue-800">MB Bank · 1234 5678 9012</p>
                                    <p className="text-sm text-blue-700">Nội dung: <strong>{invoice.code}</strong></p>
                                    <p className="mt-1 text-lg font-bold text-blue-900">{fmt(invoice.amount)}</p>
                                </div>
                            )}

                            <Button onClick={() => setStep("confirm")} className="w-full">
                                Tiếp tục →
                            </Button>
                        </div>
                    )}

                    {/* Step: Confirm */}
                    {step === "confirm" && (
                        <div className="space-y-4">
                            <div className="rounded-2xl bg-slate-50 p-4 space-y-2">
                                <SummaryRow label="Mã hóa đơn" value={invoice.code} />
                                <SummaryRow label="Dịch vụ" value={invoice.serviceDetail} />
                                <SummaryRow label="Thú cưng" value={invoice.petName} />
                                <SummaryRow label="Phương thức" value={method} />
                                <div className="border-t border-slate-200 pt-2">
                                    <SummaryRow label="Số tiền" value={fmt(invoice.amount)} />
                                </div>
                            </div>
                            <div className="rounded-2xl bg-amber-50 border border-amber-100 px-4 py-3 text-sm text-amber-800">
                                Vui lòng kiểm tra lại thông tin trước khi xác nhận thanh toán.
                            </div>
                            <div className="flex gap-2">
                                <Button variant="outline" onClick={() => setStep("select")} className="flex-1">
                                    ← Quay lại
                                </Button>
                                <Button
                                    onClick={() => { setStep("done"); onSuccess(method); }}
                                    className="flex-1"
                                >
                                    Xác nhận thanh toán
                                </Button>
                            </div>
                        </div>
                    )}

                    {/* Step: Done */}
                    {step === "done" && (
                        <div className="flex flex-col items-center gap-4 py-4 text-center">
                            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100">
                                <CheckCircle2 className="h-8 w-8 text-emerald-500" />
                            </div>
                            <div>
                                <p className="font-bold text-slate-800">{fmt(invoice.amount)}</p>
                                <p className="mt-1 text-sm text-slate-500">
                                    {invoice.serviceDetail} · {method}
                                </p>
                                <p className="mt-1 text-xs text-slate-400">
                                    {new Date().toLocaleString("vi-VN")}
                                </p>
                            </div>
                            <div className="w-full rounded-2xl border border-dashed border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
                                <Receipt className="mx-auto mb-2 h-5 w-5" />
                                Biên lai điện tử đã được gửi qua email đã đăng ký.
                            </div>
                            <Button onClick={onClose} className="w-full">
                                Đóng
                            </Button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export function PaymentsPage() {
    const [invoices, setInvoices] = useState<Invoice[]>(MOCK_INVOICES);
    const [selected, setSelected] = useState<Invoice | null>(null);
    const [filterStatus, setFilterStatus] = useState<string>("Tất cả");

    const totalPaid = invoices
        .filter((i) => i.status === "Đã thanh toán")
        .reduce((s, i) => s + i.amount, 0);
    const pendingCount = invoices.filter((i) => i.status === "Chờ thanh toán").length;
    const pendingTotal = invoices
        .filter((i) => i.status === "Chờ thanh toán")
        .reduce((s, i) => s + i.amount, 0);

    // FIX #9: Tính dịch vụ sử dụng nhiều nhất động từ dữ liệu thực tế
    const serviceCount = invoices.reduce<Record<ServiceType, number>>(
        (acc, i) => { acc[i.serviceType] = (acc[i.serviceType] ?? 0) + 1; return acc; },
        {} as Record<ServiceType, number>
    );
    const topService = (Object.entries(serviceCount) as [ServiceType, number][])
        .sort((a, b) => b[1] - a[1])[0];
    const topServiceName = topService ? topService[0] : "N/A";
    const topServiceCount = topService ? topService[1] : 0;

    const handlePaySuccess = (method: PayMethod) => {
        if (!selected) return;
        const now = new Date().toLocaleString("vi-VN");
        setInvoices((prev) =>
            prev.map((inv) =>
                inv.id === selected.id
                    ? { ...inv, status: "Đã thanh toán", paidAt: now, payMethod: method }
                    : inv
            )
        );
    };

    const filtered =
        filterStatus === "Tất cả"
            ? invoices
            : invoices.filter((i) => i.status === filterStatus);

    return (
        <div className="space-y-6">
            {/* Stats */}
            <MiniGridStats
                items={[
                    {
                        label: "Chờ thanh toán",
                        value: String(pendingCount),
                        hint: pendingCount > 0 ? `Còn ${fmt(pendingTotal)}` : "Không có hóa đơn nào",
                        icon: Clock3,
                    },
                    {
                        label: "Tổng đã thanh toán",
                        value: fmt(totalPaid).replace("₫", ""),
                        hint: "₫ — tính từ đầu năm",
                        icon: Wallet,
                    },
                    {
                        label: "Đã thanh toán",
                        value: String(invoices.filter((i) => i.status === "Đã thanh toán").length),
                        hint: "hóa đơn hoàn tất",
                        icon: CheckCircle2,
                    },
                    {
                        label: "Dịch vụ nhiều nhất",
                        // FIX #9: Tính động từ invoices
                        value: topServiceName,
                        hint: topServiceCount > 0 ? `${topServiceCount} lần sử dụng` : "Chưa có dữ liệu",
                        icon: Scissors,
                    },
                ]}
            />

            {/* Filter tabs */}
            <div className="flex gap-2 flex-wrap">
                {["Tất cả", "Chờ thanh toán", "Đã thanh toán"].map((tab) => (
                    <button
                        key={tab}
                        onClick={() => setFilterStatus(tab)}
                        className={[
                            "rounded-xl px-4 py-2 text-sm font-medium transition-all",
                            filterStatus === tab
                                ? "bg-violet-600 text-white shadow-sm"
                                : "bg-slate-100 text-slate-600 hover:bg-slate-200",
                        ].join(" ")}
                    >
                        {tab}
                        {tab === "Chờ thanh toán" && pendingCount > 0 && (
                            <span className="ml-2 rounded-full bg-amber-400 px-1.5 py-0.5 text-xs font-bold text-white">
                                {pendingCount}
                            </span>
                        )}
                    </button>
                ))}
            </div>

            {/* Invoice list */}
            <div className="space-y-3">
                {filtered.length === 0 && (
                    <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-10 text-center text-slate-500">
                        Không có hóa đơn nào.
                    </div>
                )}
                {filtered.map((inv) => (
                    <div
                        key={inv.id}
                        className="rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-violet-200 hover:shadow-sm"
                    >
                        <div className="flex items-start justify-between gap-4">
                            {/* Left info */}
                            <div className="flex items-start gap-3 min-w-0">
                                <div
                                    className={[
                                        "flex h-10 w-10 shrink-0 items-center justify-center rounded-xl",
                                        inv.serviceType === "Spa"
                                            ? "bg-blue-100 text-blue-600"
                                            : inv.serviceType === "Lưu trú"
                                              ? "bg-amber-100 text-amber-600"
                                              : "bg-emerald-100 text-emerald-600",
                                    ].join(" ")}
                                >
                                    {SERVICE_ICON[inv.serviceType]}
                                </div>
                                <div className="min-w-0">
                                    <div className="flex items-center gap-2">
                                        <span className="font-mono text-xs text-slate-400">{inv.code}</span>
                                        <Tag tone={SERVICE_TONE[inv.serviceType]}>{inv.serviceType}</Tag>
                                    </div>
                                    <p className="mt-0.5 font-semibold text-slate-800">{inv.serviceDetail}</p>
                                    <p className="text-sm text-slate-500">{inv.petName} · {inv.date}</p>
                                    {inv.paidAt && (
                                        <p className="mt-1 text-xs text-slate-400">
                                            Đã thanh toán lúc {inv.paidAt} · {inv.payMethod}
                                        </p>
                                    )}
                                </div>
                            </div>

                            {/* Right: amount + action */}
                            <div className="shrink-0 text-right">
                                <p className="text-lg font-bold text-slate-800">{fmt(inv.amount)}</p>
                                <Tag tone={STATUS_TONE[inv.status]} className="mt-1">
                                    {inv.status}
                                </Tag>
                                {inv.status === "Chờ thanh toán" && (
                                    <Button
                                        onClick={() => setSelected(inv)}
                                        className="mt-2 text-xs py-1.5 px-3"
                                    >
                                        Thanh toán ngay
                                    </Button>
                                )}
                                {inv.status === "Đã thanh toán" && (
                                    <button className="mt-2 flex items-center gap-1 text-xs text-violet-600 hover:underline">
                                        Xem biên lai
                                        <ChevronRight className="h-3 w-3" />
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Payment modal */}
            {selected && (
                <PaymentModal
                    invoice={selected}
                    onClose={() => setSelected(null)}
                    onSuccess={(method) => {
                        handlePaySuccess(method);
                        setTimeout(() => setSelected(null), 2500);
                    }}
                />
            )}
        </div>
    );
}
