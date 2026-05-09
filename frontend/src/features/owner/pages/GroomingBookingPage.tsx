import { useState } from "react";
import { Scissors, Clock, PawPrint, CheckCircle2, AlertCircle, ChevronRight } from "lucide-react";
import { Button, Input, Select, Tag, Textarea } from "~/components/atoms";
import { Card, SummaryRow } from "~/components/molecules";
import { Stepper } from "~/components/molecules/Stepper";

// ─── Mock Data ──────────────────────────────────────────────────────────────
const mockPets = ["Milu (Poodle)", "Mít (Mèo ALN)", "Bơ (Corgi)"];

const mockServices = [
    { name: "Tắm + Sấy cơ bản", price: "150.000₫", duration: "60 phút" },
    { name: "Tắm + Sấy + Cắt tỉa", price: "250.000₫", duration: "90 phút" },
    { name: "Spa Premium (full option)", price: "450.000₫", duration: "120 phút" },
    { name: "Cắt móng", price: "50.000₫", duration: "15 phút" },
    { name: "Vệ sinh tai + răng", price: "80.000₫", duration: "20 phút" },
];

const mockSlots = [
    { time: "08:00 - 09:00", available: true },
    { time: "09:00 - 10:00", available: true },
    { time: "10:00 - 11:00", available: false },
    { time: "13:00 - 14:00", available: true },
    { time: "14:00 - 15:00", available: true },
    { time: "15:00 - 16:00", available: false },
    { time: "16:00 - 17:00", available: true },
];

const steps = [
    { label: "Chọn Dịch Vụ" },
    { label: "Ngày & Giờ" },
    { label: "Xác Nhận" },
];

// ─── Main Component ──────────────────────────────────────────────────────────
export function GroomingBookingPage() {
    const [step, setStep] = useState(0);
    const [selectedPet, setSelectedPet] = useState("");
    const [selectedService, setSelectedService] = useState<(typeof mockServices)[0] | null>(null);
    const [selectedDate, setSelectedDate] = useState("");
    const [selectedSlot, setSelectedSlot] = useState("");
    const [note, setNote] = useState("");
    const [submitted, setSubmitted] = useState(false);

    // Validation per step
    const step0Valid = selectedPet && selectedService;
    const step1Valid = selectedDate && selectedSlot;

    const today = new Date().toISOString().split("T")[0];

    if (submitted) {
        return (
            <div className="flex flex-col items-center justify-center py-20 text-center animate-in fade-in slide-in-from-bottom-4">
                <div className="rounded-full bg-emerald-50 p-6 mb-6">
                    <CheckCircle2 className="h-16 w-16 text-emerald-500" />
                </div>
                <h2 className="text-2xl font-bold">Đăng ký thành công!</h2>
                <p className="mt-2 text-slate-500 max-w-sm">
                    Phiếu dịch vụ <span className="font-semibold text-slate-700">#SPA{Math.floor(Math.random() * 900 + 100)}</span> đã được tạo ở trạng thái{" "}
                    <span className="font-semibold text-amber-600">Chờ tiếp nhận</span>.
                </p>
                <div className="mt-4 rounded-2xl bg-slate-50 border border-slate-200 p-5 text-sm text-left w-full max-w-sm space-y-2">
                    <SummaryRow label="Thú cưng" value={selectedPet} />
                    <SummaryRow label="Dịch vụ" value={selectedService?.name ?? ""} />
                    <SummaryRow label="Ngày" value={selectedDate} />
                    <SummaryRow label="Giờ" value={selectedSlot} />
                    <SummaryRow label="Chi phí" value={selectedService?.price ?? ""} />
                </div>
                <div className="mt-6 flex gap-3">
                    <Button variant="outline" onClick={() => { setSubmitted(false); setStep(0); setSelectedPet(""); setSelectedService(null); setSelectedDate(""); setSelectedSlot(""); setNote(""); }}>
                        Đặt lịch mới
                    </Button>
                    <Button>Xem danh sách lịch</Button>
                </div>
            </div>
        );
    }

    return (
        <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
            {/* ── Left: Main form ── */}
            <div className="space-y-6">
                {/* Page header */}
                <div className="flex items-center gap-3">
                    <div className="rounded-2xl bg-violet-50 p-2.5">
                        <Scissors className="h-5 w-5 text-violet-500" />
                    </div>
                    <div>
                        <h2 className="text-xl font-semibold tracking-tight">Đăng ký dịch vụ làm đẹp</h2>
                        <p className="text-sm text-slate-500">Đặt lịch spa & chăm sóc cho thú cưng của bạn</p>
                    </div>
                </div>

                {/* Stepper */}
                <div className="rounded-2xl bg-white p-5 shadow-sm border border-slate-200">
                    <Stepper steps={steps} currentStep={step} />
                </div>

                {/* Step content */}
                <Card className="min-h-[400px]">
                    {/* ── Step 0: Chọn Dịch Vụ ── */}
                    {step === 0 && (
                        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-2">
                            <div>
                                <h3 className="text-lg font-semibold">Chọn thú cưng & dịch vụ</h3>
                                <p className="mt-1 text-sm text-slate-500">Chọn thú cưng và gói dịch vụ làm đẹp phù hợp.</p>
                            </div>

                            <Select
                                label="Thú cưng cần sử dụng dịch vụ"
                                options={["Chọn thú cưng...", ...mockPets]}
                                value={selectedPet}
                                onChange={(e) => setSelectedPet(e.target.value)}
                                required
                            />

                            <div>
                                <p className="mb-3 text-sm font-medium text-slate-700">
                                    Dịch vụ làm đẹp <span className="text-rose-500">*</span>
                                </p>
                                <div className="grid gap-3 sm:grid-cols-2">
                                    {mockServices.map((svc) => (
                                        <button
                                            key={svc.name}
                                            onClick={() => setSelectedService(svc)}
                                            className={[
                                                "group rounded-2xl border p-4 text-left transition hover:border-violet-300 hover:bg-violet-50",
                                                selectedService?.name === svc.name
                                                    ? "border-violet-400 bg-violet-50 ring-1 ring-violet-300"
                                                    : "border-slate-200 bg-white",
                                            ].join(" ")}
                                        >
                                            <div className="flex items-start justify-between gap-2">
                                                <p className="font-medium text-slate-800 text-sm">{svc.name}</p>
                                                {selectedService?.name === svc.name && (
                                                    <CheckCircle2 className="h-4 w-4 text-violet-500 shrink-0" />
                                                )}
                                            </div>
                                            <div className="mt-2 flex items-center gap-3 text-xs text-slate-500">
                                                <span className="font-semibold text-emerald-600">{svc.price}</span>
                                                <span>•</span>
                                                <span className="flex items-center gap-1">
                                                    <Clock className="h-3 w-3" /> {svc.duration}
                                                </span>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            </div>
                        </div>
                    )}

                    {/* ── Step 1: Ngày & Giờ ── */}
                    {step === 1 && (
                        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-2">
                            <div>
                                <h3 className="text-lg font-semibold">Chọn ngày & khung giờ</h3>
                                <p className="mt-1 text-sm text-slate-500">
                                    Khung giờ màu xám đã kín. Vui lòng chọn khung giờ còn trống.
                                </p>
                            </div>

                            <Input
                                label="Ngày sử dụng dịch vụ"
                                type="date"
                                required
                                min={today}
                                value={selectedDate}
                                onChange={(e) => { setSelectedDate(e.target.value); setSelectedSlot(""); }}
                            />

                            {selectedDate && (
                                <div className="animate-in fade-in slide-in-from-bottom-1">
                                    <p className="mb-3 text-sm font-medium text-slate-700">
                                        Khung giờ sử dụng <span className="text-rose-500">*</span>
                                    </p>
                                    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
                                        {mockSlots.map((slot) => (
                                            <button
                                                key={slot.time}
                                                disabled={!slot.available}
                                                onClick={() => setSelectedSlot(slot.time)}
                                                className={[
                                                    "rounded-2xl border px-3 py-2.5 text-sm font-medium transition",
                                                    !slot.available
                                                        ? "cursor-not-allowed border-slate-100 bg-slate-50 text-slate-300"
                                                        : selectedSlot === slot.time
                                                          ? "border-violet-400 bg-violet-50 text-violet-700 ring-1 ring-violet-300"
                                                          : "border-slate-200 bg-white text-slate-700 hover:border-violet-300 hover:bg-violet-50",
                                                ].join(" ")}
                                            >
                                                {slot.available ? slot.time : <span className="line-through">{slot.time}</span>}
                                                {!slot.available && (
                                                    <p className="text-[10px] font-normal text-slate-400 mt-0.5">Đã kín</p>
                                                )}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            )}

                            <Textarea
                                label="Ghi chú thêm (tuỳ chọn)"
                                placeholder="Bé sợ máy sấy, ưu tiên cắt móng trước, dị ứng với..."
                                rows={3}
                                value={note}
                                onChange={(e) => setNote(e.target.value)}
                            />
                        </div>
                    )}

                    {/* ── Step 2: Xác Nhận ── */}
                    {step === 2 && (
                        <div className="space-y-5 animate-in fade-in slide-in-from-bottom-2">
                            <div>
                                <h3 className="text-lg font-semibold">Xác nhận đăng ký</h3>
                                <p className="mt-1 text-sm text-slate-500">Kiểm tra kỹ thông tin trước khi xác nhận.</p>
                            </div>

                            {/* Info block */}
                            <div className="rounded-2xl border border-slate-200 bg-slate-50 divide-y divide-slate-200 text-sm">
                                <div className="flex items-center gap-3 p-4">
                                    <PawPrint className="h-4 w-4 text-violet-400 shrink-0" />
                                    <div className="flex-1 flex items-center justify-between">
                                        <span className="text-slate-500">Thú cưng</span>
                                        <span className="font-semibold">{selectedPet}</span>
                                    </div>
                                </div>
                                <div className="flex items-center gap-3 p-4">
                                    <Scissors className="h-4 w-4 text-violet-400 shrink-0" />
                                    <div className="flex-1 flex items-center justify-between">
                                        <span className="text-slate-500">Dịch vụ</span>
                                        <span className="font-semibold">{selectedService?.name}</span>
                                    </div>
                                </div>
                                <div className="flex items-center gap-3 p-4">
                                    <Clock className="h-4 w-4 text-violet-400 shrink-0" />
                                    <div className="flex-1 flex items-center justify-between">
                                        <span className="text-slate-500">Thời gian</span>
                                        <span className="font-semibold">{selectedDate} • {selectedSlot}</span>
                                    </div>
                                </div>
                                {note && (
                                    <div className="p-4">
                                        <p className="text-slate-500">Ghi chú: <span className="text-slate-700">{note}</span></p>
                                    </div>
                                )}
                            </div>

                            {/* Warning box */}
                            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 flex gap-3">
                                <AlertCircle className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-sm font-semibold text-amber-700">Lưu ý trước khi xác nhận</p>
                                    <p className="mt-1 text-xs text-amber-600">
                                        Nếu cần hủy hoặc đổi lịch, vui lòng thông báo trước ít nhất 2 giờ. Chúng tôi sẽ liên hệ xác nhận nếu có thay đổi.
                                    </p>
                                </div>
                            </div>
                        </div>
                    )}
                </Card>

                {/* Navigation bar */}
                <div className="flex items-center justify-between border-t border-slate-200 pt-5">
                    <Button variant="outline" onClick={() => setStep((s) => s - 1)} disabled={step === 0}>
                        Quay lại
                    </Button>
                    <div className="flex items-center gap-2">
                        {step < 2 && (
                            <Button
                                variant="primary"
                                onClick={() => setStep((s) => s + 1)}
                                disabled={(step === 0 && !step0Valid) || (step === 1 && !step1Valid)}
                            >
                                <span className="inline-flex items-center gap-1.5">
                                    Tiếp tục <ChevronRight className="h-4 w-4" />
                                </span>
                            </Button>
                        )}
                        {step === 2 && (
                            <Button variant="primary" onClick={() => setSubmitted(true)}>
                                Xác nhận đăng ký
                            </Button>
                        )}
                    </div>
                </div>
            </div>

            {/* ── Right: Summary sidebar ── */}
            <div className="sticky top-24 h-fit space-y-4">
                <Card title="Tóm tắt đơn dịch vụ">
                    <div className="space-y-3 text-sm">
                        <SummaryRow label="Thú cưng" value={selectedPet || "Chưa chọn"} />
                        <SummaryRow label="Dịch vụ" value={selectedService?.name || "Chưa chọn"} />
                        <SummaryRow label="Ngày" value={selectedDate || "---"} />
                        <SummaryRow label="Giờ" value={selectedSlot || "---"} />
                        <div className="h-px bg-slate-200 my-1" />
                        <div className="flex items-center justify-between font-semibold">
                            <span>Chi phí</span>
                            <span className="text-emerald-600">{selectedService?.price || "---"}</span>
                        </div>
                        <div className="flex items-center justify-between text-slate-500">
                            <span>Thời gian thực hiện</span>
                            <span>{selectedService?.duration || "---"}</span>
                        </div>
                    </div>
                </Card>

                <Card title="Trạng thái">
                    <div className="flex items-center gap-2">
                        <div className="h-2 w-2 rounded-full bg-amber-400" />
                        <span className="text-sm text-slate-600">Sẽ tạo ở trạng thái</span>
                    </div>
                    <Tag tone="amber" >Chờ tiếp nhận</Tag>
                </Card>
            </div>
        </div>
    );
}
