import { useState } from "react";
import { Scissors, Bell, Clock, PawPrint, AlertCircle, CheckCircle2 } from "lucide-react";
import { Tag } from "~/components/atoms";
import { SectionTitle } from "~/components/molecules";

// ─── Types ───────────────────────────────────────────────────────────────────
type Status = "Chờ làm" | "Đang dùng dịch vụ" | "Hoàn thành";

interface ServiceTicket {
    id: string;
    petName: string;
    ownerName: string;
    service: string;
    slot: string;
    note?: string;
    status: Status;
    updatedAt: string;
}

// ─── Mock Data ───────────────────────────────────────────────────────────────
const initialTickets: ServiceTicket[] = [
    { id: "SPA101", petName: "Milu", ownerName: "Nguyễn Văn A", service: "Tắm + Sấy + Cắt tỉa", slot: "09:00 - 10:00", note: "Bé sợ máy sấy", status: "Chờ làm", updatedAt: "08:45" },
    { id: "SPA102", petName: "Luna", ownerName: "Trần Thị B", service: "Cắt móng", slot: "09:00 - 10:00", status: "Chờ làm", updatedAt: "08:52" },
    { id: "SPA103", petName: "Mít", ownerName: "Phạm Văn C", service: "Spa Premium", slot: "09:00 - 10:30", note: "Dị ứng với mùi lavender", status: "Đang dùng dịch vụ", updatedAt: "09:10" },
    { id: "SPA104", petName: "Bơ", ownerName: "Lê Thị D", service: "Tắm + Sấy cơ bản", slot: "08:00 - 09:00", status: "Hoàn thành", updatedAt: "09:05" },
    { id: "SPA105", petName: "Táo", ownerName: "Hoàng Văn E", service: "Vệ sinh tai + răng", slot: "10:00 - 10:30", status: "Chờ làm", updatedAt: "09:30" },
];

// ─── Config ──────────────────────────────────────────────────────────────────
const COLUMNS: Status[] = ["Chờ làm", "Đang dùng dịch vụ", "Hoàn thành"];

const columnConfig: Record<Status, { tone: "amber" | "blue" | "green"; bg: string; accent: string; dotColor: string; icon: React.ReactNode }> = {
    "Chờ làm": {
        tone: "amber",
        bg: "bg-amber-50 border-amber-200",
        accent: "text-amber-600",
        dotColor: "bg-amber-400",
        icon: <Clock className="h-4 w-4 text-amber-500" />,
    },
    "Đang dùng dịch vụ": {
        tone: "blue",
        bg: "bg-blue-50 border-blue-200",
        accent: "text-blue-600",
        dotColor: "bg-blue-400",
        icon: <Scissors className="h-4 w-4 text-blue-500" />,
    },
    "Hoàn thành": {
        tone: "green",
        bg: "bg-emerald-50 border-emerald-200",
        accent: "text-emerald-600",
        dotColor: "bg-emerald-400",
        icon: <CheckCircle2 className="h-4 w-4 text-emerald-500" />,
    },
};

const TRANSITIONS: Record<Status, Status | null> = {
    "Chờ làm": "Đang dùng dịch vụ",
    "Đang dùng dịch vụ": "Hoàn thành",
    "Hoàn thành": null,
};

const transitionLabels: Record<Status, string> = {
    "Chờ làm": "Bắt đầu làm",
    "Đang dùng dịch vụ": "Hoàn thành",
    "Hoàn thành": "",
};

// ─── Popup component ─────────────────────────────────────────────────────────
function CompletionPopup({ ticket, onClose }: { ticket: ServiceTicket; onClose: () => void }) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-sm animate-in fade-in">
            <div className="mx-4 w-full max-w-sm rounded-3xl bg-white p-6 shadow-xl animate-in slide-in-from-bottom-4">
                <div className="flex flex-col items-center text-center">
                    <div className="mb-4 rounded-full bg-emerald-50 p-4">
                        <Bell className="h-8 w-8 text-emerald-500" />
                    </div>
                    <h3 className="text-lg font-bold">Dịch vụ hoàn thành!</h3>
                    <p className="mt-2 text-sm text-slate-500">
                        Thú cưng <span className="font-semibold text-slate-700">{ticket.petName}</span> đã hoàn tất{" "}
                        <span className="font-semibold">{ticket.service}</span>.
                    </p>
                    <div className="mt-4 w-full rounded-2xl bg-slate-50 border border-slate-200 p-4 text-sm text-left space-y-1.5">
                        <div className="flex justify-between">
                            <span className="text-slate-500">Phiếu DV</span>
                            <span className="font-medium">#{ticket.id}</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-slate-500">Chủ nuôi</span>
                            <span className="font-medium">{ticket.ownerName}</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-slate-500">Hoàn thành lúc</span>
                            <span className="font-medium">{new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}</span>
                        </div>
                    </div>
                    <p className="mt-3 text-xs text-emerald-600 font-medium">📢 Lễ tân đã được thông báo để gọi khách đón!</p>
                    <button
                        onClick={onClose}
                        className="mt-5 w-full rounded-2xl bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-emerald-700 transition"
                    >
                        Đã hiểu
                    </button>
                </div>
            </div>
        </div>
    );
}

// ─── Ticket Card ─────────────────────────────────────────────────────────────
function TicketCard({ ticket, onAdvance }: { ticket: ServiceTicket; onAdvance: (id: string) => void }) {
    const nextStatus = TRANSITIONS[ticket.status];
    const config = columnConfig[ticket.status];

    return (
        <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm hover:shadow-md transition-shadow">
            {/* Header */}
            <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2">
                    <div className="rounded-full bg-violet-50 p-1.5">
                        <PawPrint className="h-3.5 w-3.5 text-violet-500" />
                    </div>
                    <div>
                        <p className="font-semibold text-sm">{ticket.petName}</p>
                        <p className="text-xs text-slate-400">{ticket.ownerName}</p>
                    </div>
                </div>
                <Tag tone={config.tone}>{ticket.status}</Tag>
            </div>

            {/* Service info */}
            <div className="mt-3 rounded-xl bg-slate-50 p-3 space-y-1.5 text-xs">
                <div className="flex items-center gap-2 text-slate-600">
                    <Scissors className="h-3 w-3 text-slate-400" />
                    <span className="font-medium">{ticket.service}</span>
                </div>
                <div className="flex items-center gap-2 text-slate-500">
                    <Clock className="h-3 w-3 text-slate-400" />
                    <span>{ticket.slot}</span>
                    <span className="ml-auto text-slate-400">cập nhật {ticket.updatedAt}</span>
                </div>
            </div>

            {/* Note */}
            {ticket.note && (
                <div className="mt-2.5 flex items-start gap-1.5 rounded-xl border border-amber-200 bg-amber-50 p-2.5">
                    <AlertCircle className="h-3 w-3 text-amber-400 shrink-0 mt-0.5" />
                    <p className="text-xs text-amber-700">{ticket.note}</p>
                </div>
            )}

            {/* Ticket ID + action */}
            <div className="mt-3 flex items-center justify-between">
                <span className="rounded-lg bg-slate-100 px-2 py-0.5 text-xs text-slate-500 font-mono">#{ticket.id}</span>
                {nextStatus && (
                    <button
                        onClick={() => onAdvance(ticket.id)}
                        className={[
                            "rounded-xl px-3 py-1.5 text-xs font-semibold transition-all active:scale-95",
                            ticket.status === "Chờ làm"
                                ? "bg-blue-100 text-blue-700 hover:bg-blue-200"
                                : "bg-emerald-100 text-emerald-700 hover:bg-emerald-200",
                        ].join(" ")}
                    >
                        {transitionLabels[ticket.status]} →
                    </button>
                )}
                {!nextStatus && (
                    <span className="flex items-center gap-1 text-xs text-emerald-600 font-medium">
                        <CheckCircle2 className="h-3 w-3" /> Xong
                    </span>
                )}
            </div>
        </div>
    );
}

// ─── Main Component ──────────────────────────────────────────────────────────
export function GroomingBoardPage() {
    const [tickets, setTickets] = useState<ServiceTicket[]>(initialTickets);
    const [popup, setPopup] = useState<ServiceTicket | null>(null);

    const advanceTicket = (id: string) => {
        setTickets((prev) =>
            prev.map((t) => {
                if (t.id !== id) return t;
                const next = TRANSITIONS[t.status];
                if (!next) return t;
                const updated = {
                    ...t,
                    status: next,
                    updatedAt: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
                };
                if (next === "Hoàn thành") setPopup(updated);
                return updated;
            })
        );
    };

    return (
        <div className="space-y-6">
            {/* Popup */}
            {popup && <CompletionPopup ticket={popup} onClose={() => setPopup(null)} />}

            {/* Header */}
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <SectionTitle
                    title="Bảng điều phối dịch vụ làm đẹp"
                    subtitle={`Hôm nay, ${new Date().toLocaleDateString("vi-VN", { weekday: "long", year: "numeric", month: "long", day: "numeric" })}`}
                />
                <div className="flex items-center gap-2 text-sm text-slate-500 bg-white border border-slate-200 rounded-2xl px-4 py-2">
                    <div className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
                    Cập nhật thời gian thực
                </div>
            </div>

            {/* Stats row */}
            <div className="grid grid-cols-3 gap-3">
                {COLUMNS.map((col) => {
                    const count = tickets.filter((t) => t.status === col).length;
                    const cfg = columnConfig[col];
                    return (
                        <div key={col} className={`rounded-2xl border p-4 ${cfg.bg}`}>
                            <div className="flex items-center gap-2">
                                {cfg.icon}
                                <span className="text-sm font-medium text-slate-700">{col}</span>
                            </div>
                            <p className="mt-2 text-2xl font-bold text-slate-800">{count}</p>
                            <p className="text-xs text-slate-500">phiếu</p>
                        </div>
                    );
                })}
            </div>

            {/* Kanban board */}
            <div className="grid gap-4 lg:grid-cols-3">
                {COLUMNS.map((col) => {
                    const colTickets = tickets.filter((t) => t.status === col);
                    return (
                        <div key={col}>
                            {/* Cards */}
                            <div className="space-y-3">
                                {colTickets.length === 0 ? (
                                    <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-8 text-center">
                                        <p className="text-sm text-slate-400">Không có phiếu nào</p>
                                    </div>
                                ) : (
                                    colTickets.map((t) => (
                                        <TicketCard key={t.id} ticket={t} onAdvance={advanceTicket} />
                                    ))
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
