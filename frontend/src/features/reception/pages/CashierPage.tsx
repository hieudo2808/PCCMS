import { useState } from "react";
import {
    CheckCircle2,
    Search,
    Receipt,
    BedDouble,
    Scissors,
    Stethoscope,
    Printer,
    Banknote,
    CreditCard,
    QrCode,
    Eye,
    Clock3,
} from "lucide-react";
import { Button, Select, Tag } from "~/components/atoms";
import { Card, SummaryRow } from "~/components/molecules";
import {
    type BillStatus,
    type ServiceType,
    type PayMethod,
    type Bill,
    type BillItem,
    MOCK_BILLS,
    SERVICE_TAG_TONE,
    SERVICE_COLOR,
} from "../data/cashier.mock";


function fmt(n: number) {
    return n.toLocaleString("vi-VN") + "₫";
}

function totalOf(items: BillItem[]) {
    return items.reduce((s, i) => s + i.qty * i.unitPrice, 0);
}

// ─── Service icon map (uses JSX, stays in component file) ───────────────────
const SERVICE_ICON: Record<ServiceType, React.ReactNode> = {
    Spa: <Scissors className="h-4 w-4" />,
    "L\u01b0u tr\u00fa": <BedDouble className="h-4 w-4" />,
    "Kh\u00e1m b\u1ec7nh": <Stethoscope className="h-4 w-4" />,
};

const PAY_ICONS: Record<PayMethod, React.ReactNode> = {
    "Ti\u1ec1n m\u1eb7t": <Banknote className="h-5 w-5" />,
    "Chuy\u1ec3n kho\u1ea3n": <QrCode className="h-5 w-5" />,
    "Th\u1ebb ng\u00e2n h\u00e0ng": <CreditCard className="h-5 w-5" />,
};

// ─── Main Component ───────────────────────────────────────────────────────────
export function CashierPage() {
    const [bills, setBills] = useState<Bill[]>(MOCK_BILLS);
    const [selected, setSelected] = useState<Bill | null>(null);
    const [filter, setFilter] = useState("T\u1ea5t c\u1ea3");
    const [search, setSearch] = useState("");
    const [payMethod, setPayMethod] = useState<PayMethod>("Ti\u1ec1n m\u1eb7t");
    // FIX #4 (cashier): Dùng 1 state duy nhất thay vì paid + selected.status song song
    const [paidBillId, setPaidBillId] = useState<string | null>(null);

    const pending = bills.filter((b) => b.status === "Chờ thanh toán");
    const pendingTotal = pending.reduce((s, b) => s + totalOf(b.items), 0);

    const filtered = bills.filter((b) => {
        const matchStatus = filter === "Tất cả" || b.status === filter;
        const q = search.toLowerCase();
        const matchSearch =
            !q ||
            b.ownerName.toLowerCase().includes(q) ||
            b.ownerPhone.includes(q) ||
            b.petName.toLowerCase().includes(q) ||
            b.code.toLowerCase().includes(q);
        return matchStatus && matchSearch;
    });

    const selectBill = (b: Bill) => {
        setSelected(b);
        setPaidBillId(null);
        setPayMethod("Tiền mặt");
    };

    const confirmPayment = () => {
        if (!selected) return;
        const now = new Date().toLocaleString("vi-VN");
        const updated = { ...selected, status: "Đã thanh toán" as BillStatus, paidAt: now, payMethod };
        setBills((prev) => prev.map((b) => (b.id === selected.id ? updated : b)));
        setSelected(updated);
        setPaidBillId(selected.id);
    };

    const justPaid = selected?.id === paidBillId;

    return (
        <div>
            {/* Header */}
            <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
                <div>
                    <h1 className="text-xl font-bold text-slate-800">Thu ngân</h1>
                    <p className="mt-1 text-sm text-slate-500">
                        Xử lý thanh toán hóa đơn dịch vụ tại quầy
                    </p>
                </div>
                <div className="flex gap-3">
                    <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-2.5 text-sm">
                        <span className="text-amber-800 font-medium">{pending.length} hóa đơn chờ</span>
                        <span className="ml-2 text-amber-600">· {fmt(pendingTotal)}</span>
                    </div>
                </div>
            </div>

            <div className="grid gap-6 xl:grid-cols-[1fr_400px]">
                {/* ── Left: Bill list ───────────────────────────────────────── */}
                <div>
                    {/* Search + filter */}
                    <div className="mb-4 flex flex-wrap gap-3">
                        <div className="relative flex-1 min-w-48">
                            <Search className="pointer-events-none absolute left-3 top-3 h-4 w-4 text-slate-400" />
                            <input
                                className="w-full rounded-2xl border border-slate-300 bg-white py-2.5 pl-9 pr-4 text-sm outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100"
                                placeholder="Tìm theo tên, SĐT, mã hóa đơn..."
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                            />
                        </div>
                        <Select
                            label=""
                            options={["Tất cả", "Chờ thanh toán", "Đã thanh toán"]}
                            value={filter}
                            onChange={(e) => setFilter(e.target.value)}
                        />
                    </div>

                    {/* Bills */}
                    <div className="space-y-2">
                        {filtered.length === 0 && (
                            <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-10 text-center text-slate-400">
                                Không tìm thấy hóa đơn phù hợp.
                            </div>
                        )}
                        {filtered.map((b) => {
                            const total = totalOf(b.items);
                            const isSelected = selected?.id === b.id;
                            return (
                                <button
                                    key={b.id}
                                    onClick={() => selectBill(b)}
                                    className={[
                                        "w-full rounded-2xl border-2 p-4 text-left transition-all",
                                        isSelected
                                            ? "border-violet-500 bg-violet-50"
                                            : "border-slate-200 hover:border-violet-300 hover:shadow-sm",
                                    ].join(" ")}
                                >
                                    <div className="flex items-start justify-between gap-3">
                                        <div className="flex items-start gap-3 min-w-0">
                                            <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${SERVICE_COLOR[b.serviceType]}`}>
                                                {SERVICE_ICON[b.serviceType]}
                                            </div>
                                            <div>
                                                <div className="flex items-center gap-2 flex-wrap">
                                                    <span className="font-mono text-xs text-slate-400">{b.code}</span>
                                                    {/* FIX #8: Thêm Tag loại dịch vụ */}
                                                    <Tag tone={SERVICE_TAG_TONE[b.serviceType]}>
                                                        {b.serviceType}
                                                    </Tag>
                                                </div>
                                                <p className="font-semibold text-slate-800 mt-0.5">{b.petName}</p>
                                                <p className="text-sm text-slate-500">{b.ownerName} · {b.ownerPhone}</p>
                                                <p className="text-xs text-slate-400 mt-0.5">{b.createdAt}</p>
                                            </div>
                                        </div>
                                        <div className="text-right shrink-0">
                                            <p className="font-bold text-slate-800">{fmt(total)}</p>
                                            <Tag
                                                tone={b.status === "Chờ thanh toán" ? "amber" : "green"}
                                                className="mt-1"
                                            >
                                                {b.status === "Chờ thanh toán" ? (
                                                    <><Clock3 className="mr-1 inline h-3 w-3" />Chờ thanh toán</>
                                                ) : (
                                                    <><CheckCircle2 className="mr-1 inline h-3 w-3" />Đã thanh toán</>
                                                )}
                                            </Tag>
                                            {b.paidAt && (
                                                <p className="mt-1 text-xs text-slate-400">{b.payMethod}</p>
                                            )}
                                        </div>
                                    </div>
                                </button>
                            );
                        })}
                    </div>
                </div>

                {/* ── Right: Cashier panel ──────────────────────────────────── */}
                {!selected ? (
                    <div className="flex flex-col items-center justify-center gap-3 rounded-3xl border border-dashed border-slate-200 bg-slate-50 p-12 text-center">
                        <Receipt className="h-10 w-10 text-slate-300" />
                        <p className="text-slate-500">Chọn hóa đơn để xem chi tiết và thu tiền</p>
                    </div>
                ) : (
                    <div className="space-y-4">
                        {/* Invoice detail */}
                        <Card title={`Hóa đơn ${selected.code}`}>
                            {/* Customer info */}
                            <div className="space-y-1.5 mb-4">
                                <SummaryRow label="Khách hàng" value={selected.ownerName} />
                                <SummaryRow label="Điện thoại" value={selected.ownerPhone} />
                                <SummaryRow label="Thú cưng" value={selected.petName} />
                            </div>

                            {/* Line items */}
                            <div className="rounded-2xl border border-slate-100 overflow-hidden mb-4">
                                <div className="bg-slate-50 px-4 py-2 grid grid-cols-[1fr_auto_auto] gap-2 text-xs font-medium uppercase tracking-wide text-slate-400">
                                    <span>Dịch vụ</span>
                                    <span className="text-right">SL</span>
                                    <span className="text-right">Thành tiền</span>
                                </div>
                                {selected.items.map((item, i) => (
                                    <div
                                        key={i}
                                        className="border-t border-slate-100 px-4 py-2.5 grid grid-cols-[1fr_auto_auto] gap-2 text-sm"
                                    >
                                        <span className="text-slate-700">{item.desc}</span>
                                        <span className="text-right text-slate-500">{item.qty}</span>
                                        <span className="text-right font-medium text-slate-800">
                                            {fmt(item.qty * item.unitPrice)}
                                        </span>
                                    </div>
                                ))}
                                <div className="border-t-2 border-slate-200 px-4 py-3 flex items-center justify-between bg-slate-50">
                                    <span className="font-semibold text-slate-700">Tổng cộng</span>
                                    <span className="text-lg font-bold text-violet-700">
                                        {fmt(totalOf(selected.items))}
                                    </span>
                                </div>
                            </div>

                            {/* Note */}
                            {selected.note && (
                                <div className="mb-4 rounded-xl bg-amber-50 px-3 py-2 text-xs text-amber-800">
                                    Ghi chú: {selected.note}
                                </div>
                            )}

                            {/* FIX: Dùng 1 điều kiện rõ ràng dựa vào justPaid + status */}
                            {selected.status === "Chờ thanh toán" && !justPaid ? (
                                /* Payment section */
                                <div className="space-y-3">
                                    <p className="text-sm font-medium text-slate-700">Phương thức thanh toán</p>
                                    <div className="grid grid-cols-3 gap-2">
                                        {(["Tiền mặt", "Chuyển khoản", "Thẻ ngân hàng"] as PayMethod[]).map((m) => (
                                            <button
                                                key={m}
                                                onClick={() => setPayMethod(m)}
                                                className={[
                                                    "flex flex-col items-center gap-1.5 rounded-2xl border-2 py-3 text-xs font-medium transition-all",
                                                    payMethod === m
                                                        ? "border-violet-500 bg-violet-50 text-violet-700"
                                                        : "border-slate-200 text-slate-500 hover:border-violet-300",
                                                ].join(" ")}
                                            >
                                                {PAY_ICONS[m]}
                                                {m}
                                            </button>
                                        ))}
                                    </div>

                                    {/* QR for bank transfer */}
                                    {payMethod === "Chuyển khoản" && (
                                        <div className="rounded-2xl bg-blue-50 p-3 text-center border border-blue-100">
                                            <div className="mx-auto mb-2 h-24 w-24 rounded-xl bg-white border border-blue-200 flex items-center justify-center">
                                                <QrCode className="h-16 w-16 text-blue-300" />
                                            </div>
                                            <p className="text-xs font-medium text-blue-800">
                                                MB Bank · 1234 5678 9012
                                            </p>
                                            <p className="text-xs text-blue-700">
                                                Nội dung: <strong>{selected.code}</strong>
                                            </p>
                                        </div>
                                    )}

                                    <Button onClick={confirmPayment} className="w-full">
                                        <CheckCircle2 className="mr-2 h-4 w-4" />
                                        Xác nhận thu {fmt(totalOf(selected.items))}
                                    </Button>
                                </div>
                            ) : (
                                /* Paid confirmation */
                                <div className="space-y-3">
                                    <div className="flex flex-col items-center gap-3 rounded-2xl bg-emerald-50 border border-emerald-100 p-4 text-center">
                                        <CheckCircle2 className="h-8 w-8 text-emerald-500" />
                                        <div>
                                            <p className="font-bold text-emerald-800">
                                                Đã thu {fmt(totalOf(selected.items))}
                                            </p>
                                            {selected.paidAt && (
                                                <p className="text-xs text-emerald-600 mt-1">
                                                    {selected.paidAt} · {selected.payMethod}
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                    <div className="flex gap-2">
                                        <Button variant="outline" className="flex-1 gap-2">
                                            <Printer className="h-4 w-4" />
                                            In biên lai
                                        </Button>
                                        <Button variant="outline" className="flex-1 gap-2">
                                            <Eye className="h-4 w-4" />
                                            Xem biên lai
                                        </Button>
                                    </div>
                                </div>
                            )}
                        </Card>
                    </div>
                )}
            </div>
        </div>
    );
}
