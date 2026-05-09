import { useState } from "react";
import { CheckCircle2, XCircle, Eye, Search, BedDouble, CalendarDays, LogIn, LogOut } from "lucide-react";
import { Button, Select, Tag } from "~/components/atoms";
import { Card, SummaryRow } from "~/components/molecules";
import {
    type BoardingRequest,
    MOCK_BOARDING_REQUESTS,
    STATUS_TONE,
} from "../data/boardingReception.mock";

function fmt(n: number) {
    return n.toLocaleString("vi-VN") + "₫";
}

// ─── Main Component ───────────────────────────────────────────────────────────
export function BoardingReceptionPage() {
    const [requests, setRequests] = useState<BoardingRequest[]>(MOCK_BOARDING_REQUESTS);
    const [filter, setFilter] = useState<string>("Tất cả");
    const [search, setSearch] = useState("");
    const [selected, setSelected] = useState<BoardingRequest | null>(null);

    const filtered = requests.filter((r) => {
        const matchStatus = filter === "Tất cả" || r.status === filter;
        const matchSearch =
            search === "" ||
            r.ownerName.toLowerCase().includes(search.toLowerCase()) ||
            r.ownerPhone.includes(search) ||
            r.petName.toLowerCase().includes(search.toLowerCase()) ||
            r.code.toLowerCase().includes(search.toLowerCase());
        return matchStatus && matchSearch;
    });

    const pending = requests.filter((r) => r.status === "Chờ tiếp nhận").length;

    const accept = (id: string) => {
        setRequests((prev) =>
            prev.map((r) => (r.id === id ? { ...r, status: "Đã tiếp nhận" } : r))
        );
        if (selected?.id === id) setSelected((s) => s && { ...s, status: "Đã tiếp nhận" });
    };

    const reject = (id: string) => {
        setRequests((prev) =>
            prev.map((r) => (r.id === id ? { ...r, status: "Đã từ chối" } : r))
        );
        if (selected?.id === id) setSelected((s) => s && { ...s, status: "Đã từ chối" });
    };

    // FIX #4: Xác nhận thú cưng đã vào trung tâm → Đang lưu trú
    const checkIn = (id: string) => {
        setRequests((prev) =>
            prev.map((r) => (r.id === id ? { ...r, status: "Đang lưu trú" } : r))
        );
        if (selected?.id === id) setSelected((s) => s && { ...s, status: "Đang lưu trú" });
    };

    // FIX #4: Xác nhận trả phòng → Đã trả
    const checkOut = (id: string) => {
        setRequests((prev) =>
            prev.map((r) => (r.id === id ? { ...r, status: "Đã trả" } : r))
        );
        if (selected?.id === id) setSelected((s) => s && { ...s, status: "Đã trả" });
    };

    return (
        <div>
            {/* Page header */}
            <div className="mb-6 flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-xl font-bold text-slate-800">Tiếp nhận lưu trú</h1>
                    <p className="mt-1 text-sm text-slate-500">
                        Xem và xác nhận các phiếu đặt phòng lưu trú từ chủ nuôi
                    </p>
                </div>
                {pending > 0 && (
                    <div className="flex items-center gap-2 rounded-2xl bg-amber-50 border border-amber-200 px-4 py-2.5 text-sm text-amber-800">
                        <span className="flex h-5 w-5 items-center justify-center rounded-full bg-amber-500 text-xs font-bold text-white">
                            {pending}
                        </span>
                        phiếu chờ tiếp nhận
                    </div>
                )}
            </div>

            <div className="grid gap-6 xl:grid-cols-[1fr_380px]">
                {/* ── Left: Request list ────────────────────────────────────── */}
                <div>
                    {/* Filters */}
                    <div className="mb-4 flex flex-wrap gap-3">
                        <div className="relative flex-1 min-w-48">
                            <Search className="pointer-events-none absolute left-3 top-3 h-4 w-4 text-slate-400" />
                            <input
                                className="w-full rounded-2xl border border-slate-300 bg-white py-2.5 pl-9 pr-4 text-sm outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100"
                                placeholder="Tìm theo tên, SĐT, mã phiếu, thú cưng..."
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                            />
                        </div>
                        <Select
                            label=""
                            options={["Tất cả", "Chờ tiếp nhận", "Đã tiếp nhận", "Đang lưu trú", "Đã trả", "Đã từ chối"]}
                            value={filter}
                            onChange={(e) => setFilter(e.target.value)}
                        />
                    </div>

                    {/* List */}
                    <div className="space-y-2">
                        {filtered.length === 0 ? (
                            <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-10 text-center text-slate-500">
                                Không tìm thấy phiếu nào phù hợp.
                            </div>
                        ) : (
                            filtered.map((r) => (
                                <button
                                    key={r.id}
                                    onClick={() => setSelected(r)}
                                    className={[
                                        "w-full rounded-2xl border-2 p-4 text-left transition-all",
                                        selected?.id === r.id
                                            ? "border-violet-500 bg-violet-50"
                                            : "border-slate-200 hover:border-violet-300 hover:shadow-sm",
                                    ].join(" ")}
                                >
                                    <div className="flex items-start justify-between gap-3">
                                        <div className="min-w-0">
                                            <div className="flex items-center gap-2">
                                                <span className="rounded-lg bg-slate-100 px-1.5 py-0.5 font-mono text-xs text-slate-600">
                                                    {r.code}
                                                </span>
                                                <span className="font-semibold text-slate-800">
                                                    {r.petName}
                                                </span>
                                                <span className="text-xs text-slate-400">{r.breed}</span>
                                            </div>
                                            <p className="mt-0.5 text-sm text-slate-600">
                                                {r.ownerName} · {r.ownerPhone}
                                            </p>
                                            <div className="mt-1.5 flex items-center gap-3 text-xs text-slate-500">
                                                <span className="flex items-center gap-1">
                                                    <BedDouble className="h-3 w-3" />
                                                    {r.roomType}
                                                </span>
                                                <span className="flex items-center gap-1">
                                                    <CalendarDays className="h-3 w-3" />
                                                    {r.checkIn} → {r.checkOut} ({r.nights} đêm)
                                                </span>
                                            </div>
                                        </div>
                                        <div className="shrink-0 text-right">
                                            <Tag tone={STATUS_TONE[r.status]}>{r.status}</Tag>
                                            <p className="mt-1 text-xs font-semibold text-violet-700">
                                                {fmt(r.totalPrice)}
                                            </p>
                                        </div>
                                    </div>
                                    {/* Quick actions for pending */}
                                    {r.status === "Chờ tiếp nhận" && (
                                        <div className="mt-3 flex gap-2">
                                            <button
                                                onClick={(e) => { e.stopPropagation(); accept(r.id); }}
                                                className="flex items-center gap-1.5 rounded-xl bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-600 active:scale-95"
                                            >
                                                <CheckCircle2 className="h-3.5 w-3.5" />
                                                Tiếp nhận
                                            </button>
                                            <button
                                                onClick={(e) => { e.stopPropagation(); reject(r.id); }}
                                                className="flex items-center gap-1.5 rounded-xl border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 transition hover:bg-red-100 active:scale-95"
                                            >
                                                <XCircle className="h-3.5 w-3.5" />
                                                Từ chối
                                            </button>
                                        </div>
                                    )}
                                </button>
                            ))
                        )}
                    </div>
                </div>

                {/* ── Right: Detail panel ───────────────────────────────────── */}
                {!selected ? (
                    <div className="flex flex-col items-center justify-center gap-3 rounded-3xl border border-dashed border-slate-200 bg-slate-50 p-12 text-center">
                        <Eye className="h-10 w-10 text-slate-300" />
                        <p className="text-slate-500">Chọn một phiếu để xem chi tiết</p>
                    </div>
                ) : (
                    <Card title={`Chi tiết phiếu ${selected.code}`}>
                        <div className="space-y-3">
                            {/* Status badge */}
                            <div className="flex items-center justify-between">
                                <span className="text-sm text-slate-500">Trạng thái</span>
                                <Tag tone={STATUS_TONE[selected.status]}>{selected.status}</Tag>
                            </div>

                            <div className="border-t border-slate-100 pt-3 space-y-2">
                                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Chủ nuôi</p>
                                <SummaryRow label="Tên" value={selected.ownerName} />
                                <SummaryRow label="Điện thoại" value={selected.ownerPhone} />
                            </div>

                            <div className="border-t border-slate-100 pt-3 space-y-2">
                                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Thú cưng</p>
                                <SummaryRow label="Tên" value={`${selected.petName} (${selected.breed})`} />
                            </div>

                            <div className="border-t border-slate-100 pt-3 space-y-2">
                                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Đặt phòng</p>
                                <SummaryRow label="Loại chuồng" value={selected.roomType} />
                                <SummaryRow label="Ngày gửi" value={selected.checkIn} />
                                <SummaryRow label="Ngày đón" value={selected.checkOut} />
                                <SummaryRow label="Số đêm" value={`${selected.nights} đêm`} />
                                <SummaryRow label="Tổng dự kiến" value={fmt(selected.totalPrice)} />
                            </div>

                            {selected.note && (
                                <div className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-900">
                                    <p className="mb-1 font-medium">Ghi chú từ chủ nuôi:</p>
                                    <p>{selected.note}</p>
                                </div>
                            )}

                            <div className="border-t border-slate-100 pt-3">
                                <p className="text-xs text-slate-400">Tạo lúc {selected.createdAt}</p>
                            </div>

                            {/* Action buttons */}
                            {selected.status === "Chờ tiếp nhận" && (
                                <div className="flex gap-2 pt-2">
                                    <Button
                                        onClick={() => accept(selected.id)}
                                        className="flex-1"
                                    >
                                        <CheckCircle2 className="mr-2 h-4 w-4" />
                                        Tiếp nhận
                                    </Button>
                                    <Button
                                        variant="outline"
                                        onClick={() => reject(selected.id)}
                                        className="flex-1 border-red-200 text-red-600 hover:bg-red-50"
                                    >
                                        <XCircle className="mr-2 h-4 w-4" />
                                        Từ chối
                                    </Button>
                                </div>
                            )}
                            {/* FIX #4: Nút check-in thực tế khi thú cưng đã tới trung tâm */}
                            {selected.status === "Đã tiếp nhận" && (
                                <div className="space-y-2 pt-1">
                                    <div className="flex items-center gap-2 rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-700">
                                        <CheckCircle2 className="h-4 w-4 shrink-0" />
                                        Phiếu đã tiếp nhận. Xác nhận khi thú cưng đến trung tâm.
                                    </div>
                                    <Button
                                        onClick={() => checkIn(selected.id)}
                                        className="w-full"
                                    >
                                        <LogIn className="mr-2 h-4 w-4" />
                                        Xác nhận Check-in — Thú cưng đã đến
                                    </Button>
                                </div>
                            )}
                            {/* FIX #4: Nút trả phòng khi kết thúc lưu trú */}
                            {selected.status === "Đang lưu trú" && (
                                <div className="space-y-2 pt-1">
                                    <div className="flex items-center gap-2 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                                        <CheckCircle2 className="h-4 w-4 shrink-0" />
                                        Đang lưu trú tại trung tâm.
                                    </div>
                                    <Button
                                        variant="outline"
                                        onClick={() => checkOut(selected.id)}
                                        className="w-full"
                                    >
                                        <LogOut className="mr-2 h-4 w-4" />
                                        Xác nhận Trả phòng
                                    </Button>
                                </div>
                            )}
                            {selected.status === "Đã trả" && (
                                <div className="flex items-center gap-2 rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
                                    <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-500" />
                                    Đã hoàn tất lưu trú và trả phòng.
                                </div>
                            )}
                            {selected.status === "Đã từ chối" && (
                                <div className="flex items-center gap-2 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
                                    <XCircle className="h-4 w-4 shrink-0" />
                                    Phiếu đã bị từ chối.
                                </div>
                            )}
                        </div>
                    </Card>
                )}
            </div>
        </div>
    );
}
