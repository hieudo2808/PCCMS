import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Search } from "lucide-react";
import toast from "react-hot-toast";
import { Button, Input, Select, Tag } from "~/components/atoms";
import { Card, DataTable } from "~/components/molecules";
import { appointmentApi } from "~/shared/api/appointmentApi";
import { hasAccessToken } from "~/shared/auth/tokenStorage";
import { useAuth } from "~/features/auth/context/AuthContext";
import type { AppointmentResponse, AppointmentStatus } from "~/types/appointment";

const STATUS_OPTIONS: { value: string; label: string }[] = [
    { value: "", label: "Tất cả trạng thái" },
    { value: "PENDING", label: "Chờ tiếp nhận" },
    { value: "CHECKED_IN", label: "Đang chờ khám" },
    { value: "CANCELLED", label: "Đã hủy" },
];

function statusTone(status: AppointmentStatus): "amber" | "blue" | "red" | "green" {
    switch (status) {
        case "PENDING":
            return "amber";
        case "CHECKED_IN":
            return "blue";
        case "CANCELLED":
            return "red";
        default:
            return "green";
    }
}

function formatTime(iso: string) {
    return new Date(iso).toLocaleString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function clinicTodayIso() {
    return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh" }).format(new Date());
}

export function AppointmentReceptionPage() {
    const queryClient = useQueryClient();
    const { isAuthenticated, user } = useAuth();
    const canFetch = isAuthenticated && hasAccessToken() && Boolean(user);

    const [search, setSearch] = useState("");
    const [selectedDate, setSelectedDate] = useState(clinicTodayIso);
    const [statusFilter, setStatusFilter] = useState("");
    const [quickPhone, setQuickPhone] = useState("");
    const [quickPetId, setQuickPetId] = useState("");
    const [quickVetId, setQuickVetId] = useState("");
    const [quickSymptom, setQuickSymptom] = useState("");

    const searchParams = useMemo(() => {
        const trimmed = search.trim();
        const digitsOnly = trimmed.replace(/\D/g, "");
        if (digitsOnly.length >= 3) {
            return { phone: trimmed, customerName: undefined };
        }
        if (trimmed) {
            return { phone: undefined, customerName: trimmed };
        }
        return { phone: undefined, customerName: undefined };
    }, [search]);

    const { data: appointments = [], isLoading, isError } = useQuery({
        queryKey: ["appointments", "today", selectedDate, statusFilter, searchParams],
        queryFn: () =>
            appointmentApi.listTodayAppointments({
                date: selectedDate,
                status: (statusFilter || undefined) as AppointmentStatus | undefined,
                phone: searchParams.phone,
                customerName: searchParams.customerName,
            }),
        enabled: canFetch,
    });

    const { data: vetsOnDuty = [] } = useQuery({
        queryKey: ["appointments", "vets-on-duty", selectedDate],
        queryFn: () => appointmentApi.listVetsOnDuty(selectedDate),
        enabled: canFetch,
    });

    const { data: availability } = useQuery({
        queryKey: ["appointments", "availability", selectedDate],
        queryFn: () => appointmentApi.getAvailabilitySummary(selectedDate),
        enabled: canFetch,
    });

    const [lookupPets, setLookupPets] = useState<{ id: string; name: string }[]>([]);

    const checkInMutation = useMutation({
        mutationFn: appointmentApi.checkIn,
        onSuccess: () => {
            toast.success("Tiếp nhận thành công");
            queryClient.invalidateQueries({ queryKey: ["appointments"] });
        },
        onError: () => toast.error("Không thể tiếp nhận lịch hẹn"),
    });

    const cancelMutation = useMutation({
        mutationFn: appointmentApi.cancel,
        onSuccess: () => {
            toast.success("Hủy lịch thành công");
            queryClient.invalidateQueries({ queryKey: ["appointments"] });
        },
        onError: () => toast.error("Không thể hủy lịch hẹn"),
    });

    const quickCheckInMutation = useMutation({
        mutationFn: appointmentApi.quickCheckIn,
        onSuccess: () => {
            toast.success("Tiếp nhận walk-in thành công");
            setQuickPhone("");
            setQuickPetId("");
            setQuickSymptom("");
            queryClient.invalidateQueries({ queryKey: ["appointments"] });
        },
        onError: () => toast.error("Tạo nhanh thất bại — kiểm tra SĐT và thú cưng"),
    });

    const handleLookupPets = async () => {
        if (!quickPhone.trim()) {
            toast.error("Cần nhập SĐT khi tạo nhanh");
            return;
        }
        try {
            const result = await appointmentApi.lookupCustomer(quickPhone);
            setLookupPets(result.pets);
            if (result.pets.length === 0) {
                toast.error("Không tìm thấy thú cưng cho SĐT này");
            }
        } catch {
            toast.error("Không tìm thấy khách hàng với SĐT này");
            setLookupPets([]);
        }
    };

    const petOptions = lookupPets;

    const rows = appointments.map((apt: AppointmentResponse) => {
        const actions =
            apt.statusCode === "PENDING" ? (
                <div className="flex gap-2">
                    <button
                        type="button"
                        className="text-sm font-medium text-emerald-600 hover:underline"
                        onClick={() => checkInMutation.mutate(apt.id)}
                    >
                        Tiếp nhận
                    </button>
                    <button
                        type="button"
                        className="text-sm font-medium text-red-600 hover:underline"
                        onClick={() => cancelMutation.mutate(apt.id)}
                    >
                        Hủy
                    </button>
                </div>
            ) : (
                <span className="text-sm text-slate-500">Xem</span>
            );

        return [
            apt.appointmentCode,
            formatTime(apt.scheduledStartAt),
            apt.ownerName,
            apt.ownerPhone ?? "—",
            apt.petName,
            apt.assignedVetName ?? "—",
            <Tag tone={statusTone(apt.statusCode)}>{apt.statusLabel}</Tag>,
            actions,
        ];
    });

    return (
        <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
            <Card title="Danh sách lịch hẹn">
                <div className="mb-4 grid gap-3 md:grid-cols-[160px_1fr_180px]">
                    <Input
                        label=""
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                    />
                    <div className="relative">
                        <Search className="pointer-events-none absolute left-4 top-3.5 h-4 w-4 text-slate-400" />
                        <input
                            className="w-full rounded-2xl border border-slate-300 bg-white px-11 py-3 text-sm outline-none transition focus:border-emerald-500"
                            placeholder="Tìm theo SĐT hoặc tên khách"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    </div>
                    <Select
                        label=""
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        options={STATUS_OPTIONS}
                    />
                </div>
                {isLoading ? (
                    <p className="py-8 text-center text-sm text-slate-500">Đang tải...</p>
                ) : isError ? (
                    <p className="py-8 text-center text-sm text-red-600">
                        Không tải được danh sách — thử tải lại trang hoặc đổi ngày
                    </p>
                ) : appointments.length === 0 ? (
                    <p className="py-8 text-center text-sm text-slate-500">
                        Không có lịch hẹn trong ngày {selectedDate}. Thử chọn ngày khác hoặc lọc trạng thái.
                    </p>
                ) : (
                    <DataTable
                        columns={[
                            "Mã lịch",
                            "Giờ hẹn",
                            "Khách",
                            "SĐT",
                            "Thú cưng",
                            "Bác sĩ",
                            "Trạng thái",
                            "Hành động",
                        ]}
                        rows={rows}
                    />
                )}
            </Card>

            <Card title="Tạo nhanh tại quầy" subtitle="Dành cho khách walk-in (UC013 — nhân viên lễ tân)">
                {availability && (
                    <div className="mb-4 rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
                        <p className="font-medium text-slate-900">Ngày {selectedDate}</p>
                        <p className="mt-1">
                            {availability.vetsOnDuty} bác sĩ trực · {availability.totalExamRooms} phòng khám ·{" "}
                            {availability.availableSlots} khung giờ còn trống
                        </p>
                    </div>
                )}
                <div className="space-y-4">
                    <Input
                        label="Số điện thoại"
                        placeholder="0913.123.321"
                        value={quickPhone}
                        onChange={(e) => setQuickPhone(e.target.value)}
                        required
                    />
                    <Button variant="outline" className="w-full" onClick={handleLookupPets}>
                        Tra cứu thú cưng
                    </Button>
                    <Select
                        label="Thú cưng"
                        value={quickPetId}
                        onChange={(e) => setQuickPetId(e.target.value)}
                        options={[
                            { value: "", label: "Chọn sau khi tra cứu" },
                            ...petOptions.map((p) => ({
                                value: p.id,
                                label: p.name,
                            })),
                        ]}
                    />
                    <Select
                        label="Bác sĩ"
                        value={quickVetId}
                        onChange={(e) => setQuickVetId(e.target.value)}
                        options={[
                            { value: "", label: "Hệ thống tự gán" },
                            ...vetsOnDuty.map((v) => ({
                                value: v.id,
                                label: `${v.fullName}${v.available ? " (trực)" : " (nghỉ)"}`,
                            })),
                        ]}
                    />
                    <Input
                        label="Triệu chứng (tuỳ chọn)"
                        value={quickSymptom}
                        onChange={(e) => setQuickSymptom(e.target.value)}
                    />
                    <Button
                        className="w-full py-3"
                        disabled={quickCheckInMutation.isPending}
                        onClick={() => {
                            if (!quickPhone.trim()) {
                                toast.error("Cần nhập SĐT khi tạo nhanh");
                                return;
                            }
                            if (!quickPetId) {
                                toast.error("Chọn thú cưng sau khi tra cứu");
                                return;
                            }
                            quickCheckInMutation.mutate({
                                phone: quickPhone,
                                petId: quickPetId,
                                assignedVetId: quickVetId || undefined,
                                symptomText: quickSymptom || undefined,
                            });
                        }}
                    >
                        Tiếp nhận ngay
                    </Button>
                    <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
                        Khi tiếp nhận thành công, thú cưng được đưa vào danh sách chờ khám của bác sĩ phụ trách.
                    </div>
                </div>
            </Card>
        </div>
    );
}
