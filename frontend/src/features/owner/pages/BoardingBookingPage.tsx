import { useState } from "react";
import { CalendarDays, CheckCircle2, AlertCircle, PawPrint } from "lucide-react";
import { Button, Input, Tag, Textarea } from "~/components/atoms";
import { Card, SummaryRow } from "~/components/molecules";
import { Stepper } from "~/components/molecules/Stepper";
import {
    type RoomType,
    MOCK_OWNER_PETS,
    MOCK_ROOM_TYPES,
} from "../data/boardingBooking.mock";

const steps = [
    { label: "Chọn thú cưng & ngày" },
    { label: "Loại chuồng" },
    { label: "Xác nhận" },
];

function fmt(n: number) {
    return n.toLocaleString("vi-VN") + "₫";
}

// BUG-D: Input type="date" trả về ISO (YYYY-MM-DD), cần format lại cho nhất quán với toàn app (DD/MM/YYYY)
function formatDate(iso: string): string {
    if (!iso) return "";
    const [y, m, d] = iso.split("-");
    return `${d}/${m}/${y}`;
}

function daysBetween(from: string, to: string) {
    if (!from || !to) return 0;
    return Math.max(
        0,
        Math.ceil((new Date(to).getTime() - new Date(from).getTime()) / 86_400_000)
    );
}

// ─── Main Component ──────────────────────────────────────────────────────────
export function BoardingBookingPage() {
    const [step, setStep] = useState(0);
    const [selectedPet, setSelectedPet] = useState("");
    const [checkIn, setCheckIn] = useState("");
    const [checkOut, setCheckOut] = useState("");
    const [selectedRoom, setSelectedRoom] = useState<RoomType | null>(null);
    const [note, setNote] = useState("");
    const [submitted, setSubmitted] = useState(false);

    const today = new Date().toISOString().split("T")[0];
    const nights = daysBetween(checkIn, checkOut);

    // FIX #11: Tính ngày tối thiểu cho checkOut = checkIn + 1 ngày
    const minCheckOut = checkIn
        ? new Date(new Date(checkIn).getTime() + 86_400_000).toISOString().split("T")[0]
        : today;

    // Validation
    const step0Valid = selectedPet && checkIn && checkOut && nights > 0;
    const dateError =
        checkIn && checkOut && new Date(checkOut) <= new Date(checkIn)
            // FIX #11: Message rõ ràng hơn: tối thiểu 1 đêm
            ? "Ngày đón phải sau ngày gửi ít nhất 1 ngày (tối thiểu 1 đêm)"
            : checkIn && checkIn < today
              ? "Ngày gửi không được ở quá khứ"
              : null;
    const availableRooms = MOCK_ROOM_TYPES.filter((r) => r.available > 0);
    const noRoomsAvailable = availableRooms.length === 0;

    const step1Valid = !!selectedRoom;

    // ── Success screen ────────────────────────────────────────────────────────
    if (submitted) {
        return (
            <div className="flex min-h-[60vh] flex-col items-center justify-center gap-6 text-center">
                <div className="flex h-20 w-20 items-center justify-center rounded-full bg-emerald-100">
                    <CheckCircle2 className="h-10 w-10 text-emerald-500" />
                </div>
                <div>
                    <h2 className="text-xl font-bold text-slate-800">Đặt phòng lưu trú thành công!</h2>
                    <p className="mt-2 text-slate-500">
                        Phiếu đặt phòng đã được tạo ở trạng thái{" "}
                        <span className="font-semibold text-amber-600">Chờ tiếp nhận</span>.
                    </p>
                    <p className="mt-1 text-sm text-slate-400">
                        Nhân viên lễ tân sẽ xác nhận trong thời gian sớm nhất.
                    </p>
                </div>
                <div className="w-full max-w-sm rounded-2xl border border-slate-200 bg-slate-50 p-5 text-left space-y-3">
                    <SummaryRow label="Thú cưng" value={selectedPet} />
                    {/* BUG-D: Dùng formatDate() để hiển thị DD/MM/YYYY thay vì ISO */}
                    <SummaryRow label="Ngày gửi" value={formatDate(checkIn)} />
                    <SummaryRow label="Ngày đón" value={formatDate(checkOut)} />
                    <SummaryRow label="Số đêm" value={`${nights} đêm`} />
                    <SummaryRow label="Loại chuồng" value={selectedRoom?.name ?? ""} />
                    <SummaryRow label="Tổng dự kiến" value={fmt((selectedRoom?.pricePerDay ?? 0) * nights)} />
                </div>
                <Button onClick={() => { setStep(0); setSelectedPet(""); setCheckIn(""); setCheckOut(""); setSelectedRoom(null); setNote(""); setSubmitted(false); }}>
                    Đặt phòng mới
                </Button>
            </div>
        );
    }

    return (
        <div>
            {/* Header */}
            <div className="mb-6">
                <h1 className="text-xl font-bold text-slate-800">Đặt phòng lưu trú</h1>
                <p className="mt-1 text-sm text-slate-500">Gửi thú cưng vào trung tâm chăm sóc trong thời gian bạn vắng nhà</p>
            </div>

            <Stepper steps={steps} currentStep={step} />

            <div className="mt-6 grid gap-6 xl:grid-cols-[1fr_340px]">
                {/* ── Left panel ─────────────────────────────────────────────── */}
                <div className="space-y-4">
                    {/* Step 0: Chọn thú cưng & ngày */}
                    {step === 0 && (
                        <Card title="Thông tin đặt phòng">
                            <div className="space-y-5">
                                {/* Pet selector */}
                                <div>
                                    <p className="mb-2 text-sm font-medium text-slate-700">
                                        Thú cưng <span className="text-red-500">*</span>
                                    </p>
                                    <div className="grid gap-2 sm:grid-cols-2">
                                        {MOCK_OWNER_PETS.map((pet) => (
                                            <button
                                                key={pet}
                                                onClick={() => setSelectedPet(pet)}
                                                className={[
                                                    "flex items-center gap-3 rounded-2xl border-2 px-4 py-3 text-left text-sm transition-all",
                                                    selectedPet === pet
                                                        ? "border-violet-500 bg-violet-50 text-violet-800"
                                                        : "border-slate-200 hover:border-slate-300",
                                                ].join(" ")}
                                            >
                                                <PawPrint className="h-4 w-4 shrink-0 text-violet-400" />
                                                {pet}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* Dates */}
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <Input
                                        label="Ngày gửi *"
                                        type="date"
                                        value={checkIn}
                                        min={today}
                                        onChange={(e) => { setCheckIn(e.target.value); setCheckOut(""); }}
                                    />
                                    <Input
                                        label="Ngày đón *"
                                        type="date"
                                        value={checkOut}
                                        // FIX #11: min = checkIn + 1 ngày, đảm bảo ≥ 1 đêm
                                        min={minCheckOut}
                                        onChange={(e) => setCheckOut(e.target.value)}
                                        disabled={!checkIn}
                                    />
                                </div>

                                {/* Date error */}
                                {dateError && (
                                    <div className="flex items-center gap-2 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
                                        <AlertCircle className="h-4 w-4 shrink-0" />
                                        {dateError}
                                    </div>
                                )}

                                {/* Duration preview */}
                                {nights > 0 && !dateError && (
                                    <div className="flex items-center gap-2 rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-800">
                                        <CalendarDays className="h-4 w-4 shrink-0" />
                                        Thời gian lưu trú: <strong>{nights} đêm</strong>
                                        {" "}({checkIn} → {checkOut})
                                    </div>
                                )}

                                {/* Ghi chú */}
                                <Textarea
                                    label="Ghi chú lưu trú (tùy chọn)"
                                    placeholder="Bé sợ tiếng ồn, ăn hạt lúc 7h sáng và 6h tối... (tối đa 500 ký tự)"
                                    value={note}
                                    onChange={(e) => setNote(e.target.value.slice(0, 500))}
                                />
                                <p className="text-right text-xs text-slate-400">{note.length}/500</p>
                            </div>
                        </Card>
                    )}

                    {/* Step 1: Chọn loại chuồng */}
                    {step === 1 && (
                        <Card title="Chọn loại chuồng">
                            {noRoomsAvailable ? (
                                <div className="rounded-2xl bg-red-50 p-6 text-center">
                                    <AlertCircle className="mx-auto mb-2 h-8 w-8 text-red-400" />
                                    <p className="font-semibold text-red-700">
                                        Rất tiếc, hiện không còn chuồng trống trong khoảng thời gian này
                                    </p>
                                    <p className="mt-2 text-sm text-red-500">Vui lòng quay lại và chọn ngày khác.</p>
                                    <Button variant="outline" className="mt-4" onClick={() => setStep(0)}>
                                        ← Chọn lại ngày
                                    </Button>
                                </div>
                            ) : (
                                <div className="space-y-3">
                                    {MOCK_ROOM_TYPES.map((room) => {
                                        const isSelected = selectedRoom?.id === room.id;
                                        const isFull = room.available === 0;
                                        return (
                                            <button
                                                key={room.id}
                                                disabled={isFull}
                                                onClick={() => setSelectedRoom(room)}
                                                className={[
                                                    "w-full rounded-2xl border-2 p-4 text-left transition-all",
                                                    isFull
                                                        ? "cursor-not-allowed border-slate-100 bg-slate-50 opacity-50"
                                                        : isSelected
                                                          ? "border-violet-500 bg-violet-50"
                                                          : "border-slate-200 hover:border-violet-300",
                                                ].join(" ")}
                                            >
                                                <div className="flex items-start justify-between gap-3">
                                                    <div>
                                                        <p className="font-semibold text-slate-800">{room.name}</p>
                                                        <p className="mt-1 text-sm text-slate-500">{room.desc}</p>
                                                    </div>
                                                    <div className="text-right shrink-0">
                                                        <p className="font-bold text-violet-700">
                                                            {fmt(room.pricePerDay)}<span className="text-xs font-normal text-slate-500">/đêm</span>
                                                        </p>
                                                        <Tag tone={isFull ? "amber" : room.tone} className="mt-1">
                                                            {isFull ? "Hết chỗ" : `Còn ${room.available} phòng`}
                                                        </Tag>
                                                    </div>
                                                </div>
                                                {isSelected && (
                                                    <div className="mt-3 rounded-xl bg-violet-100 px-3 py-2 text-sm text-violet-700 font-medium">
                                                        ✓ Đã chọn — Tổng dự kiến: {fmt(room.pricePerDay * nights)}
                                                    </div>
                                                )}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </Card>
                    )}

                    {/* Step 2: Xác nhận */}
                    {step === 2 && (
                        <Card title="Xác nhận thông tin đặt phòng">
                            <div className="space-y-3">
                                <SummaryRow label="Thú cưng" value={selectedPet} />
                                <SummaryRow label="Loại chuồng" value={selectedRoom?.name ?? ""} />
                                <SummaryRow label="Ngày gửi" value={checkIn} />
                                <SummaryRow label="Ngày đón" value={checkOut} />
                                <SummaryRow label="Số đêm lưu trú" value={`${nights} đêm`} />
                                <SummaryRow
                                    label="Giá mỗi đêm"
                                    value={fmt(selectedRoom?.pricePerDay ?? 0)}
                                />
                                <div className="border-t border-slate-200 pt-3">
                                    <SummaryRow
                                        label="Tổng dự kiến"
                                        value={fmt((selectedRoom?.pricePerDay ?? 0) * nights)}
                                    />
                                </div>
                                {note && (
                                    <div className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-900">
                                        <p className="font-medium mb-1">Ghi chú lưu trú:</p>
                                        <p>{note}</p>
                                    </div>
                                )}
                                <div className="rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-800">
                                    <AlertCircle className="mr-2 inline h-4 w-4" />
                                    Phiếu sẽ ở trạng thái <strong>Chờ tiếp nhận</strong> cho đến khi nhân viên xác nhận.
                                </div>
                            </div>
                        </Card>
                    )}
                </div>

                {/* ── Right summary ──────────────────────────────────────────── */}
                <div className="space-y-4">
                    <Card title="Tóm tắt">
                        <div className="space-y-3">
                            <SummaryRow label="Thú cưng" value={selectedPet || "—"} />
                            {/* BUG-D: Dùng formatDate() cho sidebar summary */}
                            <SummaryRow label="Ngày gửi" value={checkIn ? formatDate(checkIn) : "—"} />
                            <SummaryRow label="Ngày đón" value={checkOut ? formatDate(checkOut) : "—"} />
                            <SummaryRow label="Số đêm" value={nights > 0 ? `${nights} đêm` : "—"} />
                            <SummaryRow label="Loại chuồng" value={selectedRoom?.name ?? "—"} />
                            {selectedRoom && nights > 0 && (
                                <div className="border-t border-slate-100 pt-3">
                                    <SummaryRow
                                        label="Tổng dự kiến"
                                        value={fmt(selectedRoom.pricePerDay * nights)}
                                    />
                                </div>
                            )}
                        </div>
                    </Card>

                    {/* Navigation buttons */}
                    <div className="flex flex-col gap-2">
                        {step < 2 && (
                            <Button
                                disabled={step === 0 ? !step0Valid || !!dateError : !step1Valid}
                                onClick={() => setStep((s) => s + 1)}
                                className="w-full"
                            >
                                {step === 1 ? "Tiến hành xác nhận →" : "Xem chuồng trống →"}
                            </Button>
                        )}
                        {step === 2 && (
                            <Button onClick={() => setSubmitted(true)} className="w-full">
                                {/* FIX #7: Đổi icon Home → CheckCircle2 (đúng ngữ nghĩa xác nhận) */}
                                <CheckCircle2 className="mr-2 h-4 w-4" />
                                Xác nhận đặt phòng
                            </Button>
                        )}
                        {step > 0 && (
                            <Button
                                variant="outline"
                                onClick={() => setStep((s) => s - 1)}
                                className="w-full"
                            >
                                ← Quay lại
                            </Button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
