import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Loader2, LogOut, Upload } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import toast from "react-hot-toast";
import { z } from "zod";
import { Button, Input, Tag, Textarea } from "~/components/atoms";
import { Card, EmptyState, SummaryRow } from "~/components/molecules";
import { boardingApi, roomAdminApi } from "~/features/boarding/api/boardingApi";
import type { BoardingBookingResponse, BoardingStatus, CarePeriod } from "~/types/boarding";
import { clinicTodayIso } from "~/shared/utils/dateGuards";
import { parseApiError } from "~/shared/utils/errorHandlers";

const statusTone: Record<BoardingStatus, "green" | "amber" | "blue" | "red" | "default"> = {
    RESERVED: "amber",
    CHECKED_IN: "blue",
    IN_STAY: "blue",
    CHECKED_OUT: "green",
    CANCELLED: "red",
};

const careLogSchema = z.object({
    logDate: z.string().min(1, "Chọn ngày"),
    periodCode: z.enum(["MORNING", "NOON", "AFTERNOON"]),
    feedingStatus: z.string().min(1, "Nhập tình trạng ăn uống").max(120),
    hygieneStatus: z.string().min(1, "Nhập tình trạng vệ sinh").max(120),
    healthNote: z.string().max(2000).optional(),
    staffNote: z.string().max(2000).optional(),
    caption: z.string().max(500).optional(),
    images: z.instanceof(FileList).optional(),
});

type CareLogFormValues = z.infer<typeof careLogSchema>;

function formatCurrency(value?: number) {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
        value ?? 0
    );
}

function formatDateTime(value?: string) {
    if (!value) return "Chưa cập nhật";
    return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(
        new Date(value)
    );
}

function datePart(value?: string) {
    return value ? value.slice(0, 10) : undefined;
}

function minDate(first?: string, second?: string) {
    if (!first) return second;
    if (!second) return first;
    return first < second ? first : second;
}

export function BoardingLogPage() {
    const queryClient = useQueryClient();
    const [selectedBookingId, setSelectedBookingId] = useState<string | null>(null);
    const [selectedRoomId, setSelectedRoomId] = useState("");

    const bookingsQuery = useQuery({
        queryKey: ["staff-boarding-bookings"],
        queryFn: () => boardingApi.getBookings(),
    });

    const roomsQuery = useQuery({
        queryKey: ["rooms"],
        queryFn: () => roomAdminApi.getRooms(),
    });

    const bookings = bookingsQuery.data?.content ?? [];
    const selectedBooking: BoardingBookingResponse | undefined =
        bookings.find((booking) => booking.id === selectedBookingId) ?? bookings[0];

    const careLogsQuery = useQuery({
        queryKey: ["boarding-care-logs", selectedBooking?.id],
        queryFn: () => boardingApi.getCareLogs(selectedBooking!.id),
        enabled: Boolean(selectedBooking?.id),
    });

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors },
    } = useForm<CareLogFormValues>({
        resolver: zodResolver(careLogSchema),
        defaultValues: {
            periodCode: "MORNING",
            feedingStatus: "",
            hygieneStatus: "",
            healthNote: "",
            staffNote: "",
            caption: "",
        },
    });

    const invalidate = () => {
        queryClient.invalidateQueries({ queryKey: ["staff-boarding-bookings"] });
        queryClient.invalidateQueries({ queryKey: ["boarding-care-logs"] });
    };

    const confirmMutation = useMutation({
        mutationFn: () => boardingApi.confirmBooking(selectedBooking!.id, selectedRoomId),
        onSuccess: () => {
            toast.success("Đã xác nhận và gán phòng");
            setSelectedRoomId("");
            invalidate();
        },
        onError: (error) => toast.error(parseApiError(error)),
    });

    const checkInMutation = useMutation({
        mutationFn: () => boardingApi.checkIn(selectedBooking!.id),
        onSuccess: invalidate,
        onError: (error) => toast.error(parseApiError(error)),
    });

    const startStayMutation = useMutation({
        mutationFn: () => boardingApi.startStay(selectedBooking!.id),
        onSuccess: invalidate,
        onError: (error) => toast.error(parseApiError(error)),
    });

    const checkOutMutation = useMutation({
        mutationFn: () => boardingApi.checkOut(selectedBooking!.id),
        onSuccess: () => {
            toast.success("Đã check-out và tạo hóa đơn");
            invalidate();
        },
        onError: (error) => toast.error(parseApiError(error)),
    });

    const createCareLogMutation = useMutation({
        mutationFn: (values: CareLogFormValues) =>
            boardingApi.createCareLog({
                sessionId: selectedBooking!.sessionId!,
                logDate: values.logDate,
                periodCode: values.periodCode as CarePeriod,
                feedingStatus: values.feedingStatus,
                hygieneStatus: values.hygieneStatus,
                healthNote: values.healthNote,
                staffNote: values.staffNote,
                caption: values.caption,
                images: values.images ? Array.from(values.images) : [],
            }),
        onSuccess: () => {
            toast.success("Đã thêm nhật ký");
            reset();
            invalidate();
        },
        onError: (error) => toast.error(parseApiError(error)),
    });

    const availableRooms = (roomsQuery.data?.content ?? []).filter(
        (room) =>
            room.statusCode === "AVAILABLE" &&
            room.roomTypeId === selectedBooking?.requestedRoomTypeId
    );
    const careLogMinDate = datePart(selectedBooking?.expectedCheckinAt);
    const careLogMaxDate = minDate(clinicTodayIso(), datePart(selectedBooking?.expectedCheckoutAt));

    if (bookingsQuery.isLoading) {
        return (
            <div className="flex items-center gap-2 text-sm text-slate-500">
                <Loader2 className="h-4 w-4 animate-spin" /> Đang tải booking
            </div>
        );
    }

    if (bookingsQuery.isError) {
        return (
            <EmptyState title="Không thể tải booking lưu trú" description="Vui lòng thử lại sau." />
        );
    }

    return (
        <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
            <Card title="Hàng đợi lưu trú">
                {bookings.length === 0 ? (
                    <EmptyState
                        title="Chưa có booking"
                        description="Booking mới sẽ xuất hiện tại đây."
                    />
                ) : (
                    <div className="space-y-3">
                        {bookings.map((booking) => (
                            <button
                                key={booking.id}
                                type="button"
                                onClick={() => setSelectedBookingId(booking.id)}
                                className={`w-full rounded-2xl border p-4 text-left transition ${
                                    selectedBooking?.id === booking.id
                                        ? "border-primary-500 bg-primary-50"
                                        : "border-slate-200 hover:border-slate-300"
                                }`}
                            >
                                <div className="flex items-start justify-between gap-3">
                                    <div>
                                        <p className="font-semibold">{booking.petName}</p>
                                        <p className="mt-1 text-sm text-slate-500">
                                            {booking.ownerName}
                                        </p>
                                    </div>
                                    <Tag tone={statusTone[booking.statusCode]}>
                                        {booking.statusCode}
                                    </Tag>
                                </div>
                                <p className="mt-3 text-xs text-slate-500">
                                    {formatDateTime(booking.expectedCheckinAt)}
                                </p>
                            </button>
                        ))}
                    </div>
                )}
            </Card>

            {selectedBooking && (
                <div className="space-y-6">
                    <Card
                        title="Vận hành booking"
                        right={
                            <Tag tone={statusTone[selectedBooking.statusCode]}>
                                {selectedBooking.statusCode}
                            </Tag>
                        }
                    >
                        <div className="grid gap-4 md:grid-cols-2">
                            <SummaryRow label="Mã booking" value={selectedBooking.bookingCode} />
                            <SummaryRow label="Thú cưng" value={selectedBooking.petName} />
                            <SummaryRow
                                label="Loại phòng"
                                value={selectedBooking.requestedRoomTypeName}
                            />
                            <SummaryRow
                                label="Phòng"
                                value={
                                    selectedBooking.roomCode
                                        ? `${selectedBooking.roomCode} - ${selectedBooking.roomName}`
                                        : "Chưa gán"
                                }
                            />
                            <SummaryRow
                                label="Nhận phòng dự kiến"
                                value={formatDateTime(selectedBooking.expectedCheckinAt)}
                            />
                            <SummaryRow
                                label="Trả phòng dự kiến"
                                value={formatDateTime(selectedBooking.expectedCheckoutAt)}
                            />
                            <SummaryRow
                                label="Tạm tính"
                                value={formatCurrency(selectedBooking.estimatedPriceVnd)}
                            />
                            <SummaryRow
                                label="Hóa đơn"
                                value={
                                    selectedBooking.invoice
                                        ? `${selectedBooking.invoice.invoiceCode} - ${formatCurrency(selectedBooking.invoice.totalAmountVnd)}`
                                        : "Chưa tạo"
                                }
                            />
                        </div>

                        {selectedBooking.statusCode === "RESERVED" && !selectedBooking.roomId && (
                            <div className="mt-5 flex flex-col gap-3 rounded-2xl border border-slate-200 p-4 md:flex-row md:items-end">
                                <label className="flex flex-1 flex-col gap-1.5 text-[13px] font-medium text-slate-700">
                                    Phòng trống cùng loại
                                    <select
                                        value={selectedRoomId}
                                        onChange={(event) => setSelectedRoomId(event.target.value)}
                                        className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                                    >
                                        <option value="">Chọn phòng</option>
                                        {availableRooms.map((room) => (
                                            <option key={room.id} value={room.id}>
                                                {room.roomCode} - {room.name}
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                <Button
                                    disabled={!selectedRoomId || confirmMutation.isPending}
                                    onClick={() => confirmMutation.mutate()}
                                >
                                    {confirmMutation.isPending ? "Đang xác nhận" : "Xác nhận"}
                                </Button>
                            </div>
                        )}

                        <div className="mt-5 flex flex-wrap gap-2">
                            <Button
                                variant="outline"
                                disabled={
                                    selectedBooking.statusCode !== "RESERVED" ||
                                    !selectedBooking.roomId ||
                                    checkInMutation.isPending
                                }
                                onClick={() => checkInMutation.mutate()}
                            >
                                Check-in
                            </Button>
                            <Button
                                variant="outline"
                                disabled={
                                    selectedBooking.statusCode !== "CHECKED_IN" ||
                                    startStayMutation.isPending
                                }
                                onClick={() => startStayMutation.mutate()}
                            >
                                Bắt đầu lưu trú
                            </Button>
                            <Button
                                variant="secondary"
                                disabled={
                                    !["CHECKED_IN", "IN_STAY"].includes(
                                        selectedBooking.statusCode
                                    ) || checkOutMutation.isPending
                                }
                                onClick={() => checkOutMutation.mutate()}
                            >
                                <span className="inline-flex items-center gap-2">
                                    <LogOut className="h-4 w-4" /> Check-out
                                </span>
                            </Button>
                        </div>
                    </Card>

                    <Card title="Thêm nhật ký chăm sóc">
                        <form
                            className="space-y-4"
                            onSubmit={handleSubmit((values) =>
                                createCareLogMutation.mutate(values)
                            )}
                        >
                            <div className="grid gap-4 md:grid-cols-3">
                                <Input
                                    type="date"
                                    label="Ngày"
                                    min={careLogMinDate}
                                    max={careLogMaxDate}
                                    error={errors.logDate?.message}
                                    {...register("logDate")}
                                />
                                <label className="flex flex-col gap-1.5 text-[13px] font-medium text-slate-700">
                                    Buổi
                                    <select
                                        className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
                                        {...register("periodCode")}
                                    >
                                        <option value="MORNING">Sáng</option>
                                        <option value="NOON">Trưa</option>
                                        <option value="AFTERNOON">Chiều</option>
                                    </select>
                                </label>
                                <Input
                                    type="file"
                                    accept="image/png,image/jpeg,image/webp"
                                    multiple
                                    label="Ảnh"
                                    {...register("images")}
                                />
                            </div>
                            <div className="grid gap-4 md:grid-cols-2">
                                <Input
                                    label="Tình trạng ăn uống"
                                    error={errors.feedingStatus?.message}
                                    {...register("feedingStatus")}
                                />
                                <Input
                                    label="Tình trạng vệ sinh"
                                    error={errors.hygieneStatus?.message}
                                    {...register("hygieneStatus")}
                                />
                            </div>
                            <Textarea
                                label="Ghi chú sức khỏe"
                                rows={3}
                                error={errors.healthNote?.message}
                                {...register("healthNote")}
                            />
                            <Textarea
                                label="Ghi chú nhân viên"
                                rows={3}
                                error={errors.staffNote?.message}
                                {...register("staffNote")}
                            />
                            <Input
                                label="Caption ảnh"
                                error={errors.caption?.message}
                                {...register("caption")}
                            />
                            <Button
                                type="submit"
                                disabled={
                                    !selectedBooking.sessionId ||
                                    !["CHECKED_IN", "IN_STAY"].includes(
                                        selectedBooking.statusCode
                                    ) ||
                                    createCareLogMutation.isPending
                                }
                            >
                                <span className="inline-flex items-center gap-2">
                                    {createCareLogMutation.isPending ? (
                                        <Loader2 className="h-4 w-4 animate-spin" />
                                    ) : (
                                        <Upload className="h-4 w-4" />
                                    )}
                                    Lưu nhật ký
                                </span>
                            </Button>
                        </form>
                    </Card>

                    <Card
                        title="Nhật ký hiện có"
                        right={<CheckCircle2 className="h-5 w-5 text-slate-400" />}
                    >
                        {careLogsQuery.isLoading ? (
                            <div className="flex items-center gap-2 text-sm text-slate-500">
                                <Loader2 className="h-4 w-4 animate-spin" /> Đang tải nhật ký
                            </div>
                        ) : careLogsQuery.data?.length === 0 ? (
                            <EmptyState
                                title="Chưa có nhật ký"
                                description="Nhập nhật ký sau khi thú cưng check-in."
                            />
                        ) : (
                            <div className="space-y-3">
                                {careLogsQuery.data?.map((log) => (
                                    <div
                                        key={log.id}
                                        className="rounded-2xl border border-slate-200 p-4"
                                    >
                                        <p className="font-semibold">
                                            {log.logDate} - {log.periodCode}
                                        </p>
                                        <p className="mt-1 text-sm text-slate-500">
                                            {log.feedingStatus} | {log.hygieneStatus}
                                        </p>
                                        {log.media.length > 0 && (
                                            <p className="mt-2 text-xs text-slate-500">
                                                {log.media.length} ảnh đã tải lên
                                            </p>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </Card>
                </div>
            )}
        </div>
    );
  }
