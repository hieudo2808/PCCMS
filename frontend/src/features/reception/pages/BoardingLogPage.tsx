import { useState } from "react";
import { Camera, CheckCircle2, AlertCircle, Clock, PawPrint, Upload } from "lucide-react";
import { Button, Select, Tag, Textarea } from "~/components/atoms";
import { Card, SectionTitle } from "~/components/molecules";

// ─── Types ────────────────────────────────────────────────────────────────────
type Session = "Sáng" | "Trưa" | "Chiều";
type EatStatus = "Ăn tốt" | "Ăn ít" | "Bỏ ăn";
type HygieneStatus = "Bình thường" | "Theo dõi thêm" | "Bất thường";

interface LogEntry {
    session: Session;
    eat: EatStatus;
    hygiene: HygieneStatus;
    note: string;
    mediaCount: number;
    savedAt: string;
}

interface BoardingPet {
    id: string;
    cage: string;
    petName: string;
    breed: string;
    ownerName: string;
    checkIn: string;
    checkOut: string;
    dayNum: number;
    totalDays: number;
    todayLogs: Session[];
}

// ─── Mock Data ────────────────────────────────────────────────────────────────
const mockPets: BoardingPet[] = [
    {
        id: "1",
        cage: "C12",
        petName: "Milu",
        breed: "Poodle",
        ownerName: "Nguyễn Văn A",
        checkIn: "22/05/2026",
        checkOut: "26/05/2026",
        dayNum: 3,
        totalDays: 4,
        todayLogs: ["Sáng"],
    },
    {
        id: "2",
        cage: "B03",
        petName: "Bơ",
        breed: "Corgi",
        ownerName: "Lê Thị D",
        checkIn: "23/05/2026",
        checkOut: "25/05/2026",
        dayNum: 2,
        totalDays: 2,
        todayLogs: [],
    },
    {
        id: "3",
        cage: "A08",
        petName: "Mít",
        breed: "Mèo ALN",
        ownerName: "Phạm Văn C",
        checkIn: "20/05/2026",
        checkOut: "25/05/2026",
        dayNum: 5,
        totalDays: 5,
        todayLogs: ["Sáng", "Trưa", "Chiều"],
    },
];

const ALL_SESSIONS: Session[] = ["Sáng", "Trưa", "Chiều"];
const EAT_OPTIONS: EatStatus[] = ["Ăn tốt", "Ăn ít", "Bỏ ăn"];
const HYGIENE_OPTIONS: HygieneStatus[] = ["Bình thường", "Theo dõi thêm", "Bất thường"];

function sessionStatusTag(sessions: Session[]) {
    if (sessions.length === 3)
        return <Tag tone="green">Đủ 3 buổi ✓</Tag>;
    if (sessions.length === 0)
        return <Tag tone="amber">Chưa cập nhật</Tag>;
    return <Tag tone="blue">Đã {sessions.length}/3 buổi</Tag>;
}

// ─── Main Component ───────────────────────────────────────────────────────────
export function BoardingLogPage() {
    const [selectedPetId, setSelectedPetId] = useState<string | null>(null);
    const [session, setSession] = useState<Session>("Sáng");
    const [eat, setEat] = useState<EatStatus>("Ăn tốt");
    const [hygiene, setHygiene] = useState<HygieneStatus>("Bình thường");
    const [note, setNote] = useState("");
    const [mediaFiles, setMediaFiles] = useState<string[]>([]);
    const [saveSuccess, setSaveSuccess] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    const [pets, setPets] = useState<BoardingPet[]>(mockPets);
    // FIX #3: Lưu log theo petId thay vì global state — tránh mất log khi đổi thú cưng
    const [savedLogsByPet, setSavedLogsByPet] = useState<Record<string, LogEntry[]>>({});

    const pet = pets.find((p) => p.id === selectedPetId) ?? null;
    const alreadyLoggedToday = pet ? pet.todayLogs.includes(session) : false;
    // Lấy log đã lưu của pet hiện tại
    const savedLogs = selectedPetId ? (savedLogsByPet[selectedPetId] ?? []) : [];

    // FIX #6: Reset toàn bộ form (eat, hygiene, session, note, media)
    const resetForm = () => {
        setNote("");
        setMediaFiles([]);
        setSaveError(null);
        setEat("Ăn tốt");
        setHygiene("Bình thường");
        setSession("Sáng");
    };

    const handleSave = () => {
        if (!pet) return;
        if (alreadyLoggedToday) {
            // FIX #2: Sửa message nhất quán với hành vi nút bị disable
            setSaveError(`Buổi ${session} đã được cập nhật hôm nay. Không thể thay đổi.`);
            return;
        }
        if (mediaFiles.length === 0) {
            setSaveError("Vui lòng tải lên ít nhất 1 ảnh hoặc video.");
            return;
        }

        const newLog: LogEntry = {
            session,
            eat,
            hygiene,
            note,
            mediaCount: mediaFiles.length,
            savedAt: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
        };

        // FIX #3: Cập nhật log theo petId
        setSavedLogsByPet((prev) => ({
            ...prev,
            [pet.id]: [newLog, ...(prev[pet.id] ?? [])],
        }));

        setPets((prev) =>
            prev.map((p) =>
                p.id === pet.id
                    ? { ...p, todayLogs: [...p.todayLogs, session] }
                    : p
            )
        );
        setNote("");
        setMediaFiles([]);
        setSaveError(null);
        setSaveSuccess(true);
        setTimeout(() => setSaveSuccess(false), 3000);
    };

    const simulateUpload = () => {
        if (mediaFiles.length >= 5) {
            setSaveError("Đã đạt giới hạn 5 tệp. Xóa bớt để tải thêm.");
            return;
        }
        const fakeName = `anh_${Date.now()}.jpg`;
        setMediaFiles((prev) => [...prev, fakeName]);
        setSaveError(null);
    };

    return (
        <div>
            {/* Page header */}
            <div className="mb-6">
                <h1 className="text-xl font-bold text-slate-800">Nhật ký lưu trú</h1>
                <p className="mt-1 text-sm text-slate-500">
                    Cập nhật tình trạng chăm sóc thú cưng theo từng buổi trong ngày
                </p>
            </div>

            <div className="grid gap-6 xl:grid-cols-[340px_1fr]">
                {/* ── Left: Pet list ───────────────────────────────────────── */}
                <div>
                    <SectionTitle title="Thú cưng đang lưu trú" subtitle={`${pets.length} bé hôm nay`} />
                    <div className="mt-3 space-y-2">
                        {pets.map((p) => (
                            <button
                                key={p.id}
                                // FIX #3: Chỉ setSelectedPetId, không xóa savedLogs
                                onClick={() => {
                                    setSelectedPetId(p.id);
                                    setSaveSuccess(false);
                                    setSaveError(null);
                                    // FIX #6: Reset form khi chuyển thú cưng
                                    resetForm();
                                }}
                                className={[
                                    "w-full rounded-2xl border-2 p-4 text-left transition-all",
                                    selectedPetId === p.id
                                        ? "border-emerald-500 bg-emerald-50"
                                        : "border-slate-200 hover:border-emerald-300 hover:shadow-sm",
                                ].join(" ")}
                            >
                                <div className="flex items-start justify-between gap-2">
                                    <div>
                                        <p className="font-semibold text-slate-800">
                                            <span className="mr-2 rounded-lg bg-slate-100 px-1.5 py-0.5 text-xs font-mono text-slate-600">
                                                {p.cage}
                                            </span>
                                            {p.petName}
                                        </p>
                                        <p className="mt-0.5 text-xs text-slate-500">{p.breed} · {p.ownerName}</p>
                                        <p className="mt-1 text-xs text-slate-400">
                                            Ngày {p.dayNum}/{p.totalDays} · {p.checkIn} → {p.checkOut}
                                        </p>
                                    </div>
                                    <div className="shrink-0">{sessionStatusTag(p.todayLogs)}</div>
                                </div>
                            </button>
                        ))}
                    </div>
                </div>

                {/* ── Right: Log form ───────────────────────────────────────── */}
                {!pet ? (
                    <div className="flex flex-col items-center justify-center gap-3 rounded-3xl border border-dashed border-slate-200 bg-slate-50 p-16 text-center">
                        <PawPrint className="h-10 w-10 text-slate-300" />
                        <p className="text-slate-500">Chọn thú cưng bên trái để bắt đầu cập nhật nhật ký</p>
                    </div>
                ) : (
                    <div className="space-y-4">
                        <Card title={`Nhật ký của ${pet.petName} — Hôm nay`}>
                            {/* FIX #10: Tách grid sessions và ô ảnh thành 2 hàng riêng biệt */}
                            {/* Hàng 1: 3 buổi Sáng/Trưa/Chiều */}
                            <div className="mb-3 grid grid-cols-3 gap-3">
                                {ALL_SESSIONS.map((s) => (
                                    <div
                                        key={s}
                                        className={[
                                            "rounded-xl p-3 text-center text-sm border",
                                            pet.todayLogs.includes(s)
                                                ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                                                : "border-slate-200 bg-slate-50 text-slate-400",
                                        ].join(" ")}
                                    >
                                        <Clock className="mx-auto mb-1 h-4 w-4" />
                                        <p className="font-medium">{s}</p>
                                        <p className="text-xs">{pet.todayLogs.includes(s) ? "✓ Đã cập nhật" : "Chưa có"}</p>
                                    </div>
                                ))}
                            </div>
                            {/* Hàng 2: Trạng thái ảnh/video (tách riêng, không gộp vào grid 3 buổi) */}
                            <div className="mb-4 flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5">
                                <Camera className="h-4 w-4 text-slate-400" />
                                <span className="text-sm text-slate-600">Ảnh/Video hôm nay:</span>
                                <span className="text-sm font-medium text-slate-800">{mediaFiles.length} tệp mới</span>
                            </div>

                            {/* Previous logs for today — FIX #3: hiển thị log của pet hiện tại */}
                            {savedLogs.length > 0 && (
                                <div className="mb-4 space-y-2 border-t border-slate-100 pt-4">
                                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Đã lưu hôm nay</p>
                                    {savedLogs.map((log, i) => (
                                        <div key={i} className="rounded-xl bg-slate-50 p-3 text-sm">
                                            <div className="flex items-center justify-between">
                                                <span className="font-medium">Buổi {log.session}</span>
                                                <span className="text-xs text-slate-400">{log.savedAt}</span>
                                            </div>
                                            <p className="mt-1 text-slate-600">
                                                Ăn uống: <strong>{log.eat}</strong> · Vệ sinh: <strong>{log.hygiene}</strong>
                                            </p>
                                            {log.note && <p className="mt-1 text-slate-500 italic">"{log.note}"</p>}
                                            <p className="mt-1 text-xs text-slate-400">{log.mediaCount} tệp ảnh/video</p>
                                        </div>
                                    ))}
                                </div>
                            )}

                            {/* Form */}
                            <div className="border-t border-slate-100 pt-4">
                                <p className="mb-3 text-sm font-semibold text-slate-700">Thêm nhật ký buổi mới</p>

                                <div className="grid gap-4 sm:grid-cols-3">
                                    <Select
                                        label="Buổi cập nhật *"
                                        options={ALL_SESSIONS}
                                        value={session}
                                        onChange={(e) => {
                                            setSession(e.target.value as Session);
                                            setSaveError(null);
                                        }}
                                    />
                                    <Select
                                        label="Tình trạng ăn uống *"
                                        options={EAT_OPTIONS}
                                        value={eat}
                                        onChange={(e) => setEat(e.target.value as EatStatus)}
                                    />
                                    <Select
                                        label="Tình trạng vệ sinh *"
                                        options={HYGIENE_OPTIONS}
                                        value={hygiene}
                                        onChange={(e) => setHygiene(e.target.value as HygieneStatus)}
                                    />
                                </div>

                                {/* FIX #2: Message nhất quán — không ngụ ý có thể chỉnh sửa */}
                                {alreadyLoggedToday && (
                                    <div className="mt-3 flex items-center gap-2 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
                                        <AlertCircle className="h-4 w-4 shrink-0" />
                                        Buổi <strong>{session}</strong> đã được cập nhật hôm nay. Không thể thay đổi.
                                    </div>
                                )}

                                {/* Media upload */}
                                <div className="mt-4 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <p className="text-sm font-medium text-slate-700">
                                                Ảnh / Video <span className="text-red-500">*</span>
                                            </p>
                                            <p className="mt-0.5 text-xs text-slate-400">
                                                Tối đa 5 tệp, mỗi tệp không quá 10MB · JPG, PNG, MP4
                                            </p>
                                        </div>
                                        <Button
                                            variant="outline"
                                            onClick={simulateUpload}
                                            disabled={mediaFiles.length >= 5}
                                        >
                                            <Upload className="mr-2 h-4 w-4" />
                                            Tải lên
                                        </Button>
                                    </div>
                                    {mediaFiles.length > 0 && (
                                        <div className="mt-3 flex flex-wrap gap-2">
                                            {mediaFiles.map((f, i) => (
                                                <div
                                                    key={i}
                                                    className="flex items-center gap-1.5 rounded-lg bg-white border border-slate-200 px-2.5 py-1 text-xs text-slate-600"
                                                >
                                                    <Camera className="h-3 w-3 text-emerald-500" />
                                                    {f}
                                                    <button
                                                        onClick={() =>
                                                            setMediaFiles((prev) =>
                                                                prev.filter((_, idx) => idx !== i)
                                                            )
                                                        }
                                                        className="ml-1 text-slate-400 hover:text-red-500"
                                                    >
                                                        ×
                                                    </button>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                {/* Notes */}
                                <div className="mt-4">
                                    <Textarea
                                        label="Ghi chú của nhân viên (tùy chọn)"
                                        placeholder="Bé ngủ nhiều, ăn hết khẩu phần, đã đi dạo 15 phút... (tối đa 500 ký tự)"
                                        value={note}
                                        onChange={(e) => setNote(e.target.value.slice(0, 500))}
                                    />
                                    <p className="mt-1 text-right text-xs text-slate-400">{note.length}/500</p>
                                </div>

                                {/* Error / success feedback */}
                                {saveError && (
                                    <div className="mt-3 flex items-center gap-2 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
                                        <AlertCircle className="h-4 w-4 shrink-0" />
                                        {saveError}
                                    </div>
                                )}
                                {saveSuccess && (
                                    <div className="mt-3 flex items-center gap-2 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                                        <CheckCircle2 className="h-4 w-4 shrink-0" />
                                        Nhật ký đã lưu thành công và chủ nuôi có thể theo dõi ngay.
                                    </div>
                                )}

                                {/* Actions */}
                                <div className="mt-5 flex gap-2">
                                    {/* BUG-A: Disable khi chưa có ảnh — không để người dùng phải nhấn mới thấy lỗi */}
                                    <Button onClick={handleSave} disabled={alreadyLoggedToday || mediaFiles.length === 0}>
                                        Lưu nhật ký
                                    </Button>
                                    {/* FIX #6: Reset toàn bộ form — eat, hygiene, session, note, media */}
                                    <Button variant="outline" onClick={resetForm}>
                                        Làm mới
                                    </Button>
                                </div>
                            </div>
                        </Card>
                    </div>
                )}
            </div>
        </div>
    );
}
