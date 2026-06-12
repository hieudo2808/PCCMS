import { useQuery } from "@tanstack/react-query";
import { Camera, FileText, Loader2 } from "lucide-react";
import { useState } from "react";
import { Button, Tag } from "~/components/atoms";
import { Card, EmptyState, SummaryRow } from "~/components/molecules";
import { boardingApi } from "~/features/boarding/api/boardingApi";
import type { BoardingBookingResponse, BoardingStatus, CarePeriod } from "~/types/boarding";

const statusTone: Record<BoardingStatus, "green" | "amber" | "blue" | "red" | "default"> = {
    RESERVED: "amber",
    CHECKED_IN: "blue",
    IN_STAY: "blue",
    CHECKED_OUT: "green",
    CANCELLED: "red",
};

const periodLabel: Record<CarePeriod, string> = {
    MORNING: "Sáng",
    NOON: "Trưa",
    AFTERNOON: "Chiều",
};

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

export function BoardingTrackingPage() {
    const [selectedBookingId, setSelectedBookingId] = useState<string | null>(null);

    const bookingsQuery = useQuery({
        queryKey: ["my-boarding-bookings"],
        queryFn: () => boardingApi.getMyBookings(),
    });

    const bookings = bookingsQuery.data?.content ?? [];
    const selectedBooking: BoardingBookingResponse | undefined =
        bookings.find((booking) => booking.id === selectedBookingId) ?? bookings[0];

    const careLogsQuery = useQuery({
        queryKey: ["boarding-care-logs", selectedBooking?.id],
        queryFn: () => boardingApi.getCareLogs(selectedBooking!.id),
        enabled: Boolean(selectedBooking?.id),
    });

    if (bookingsQuery.isLoading) {
        return (
            <div className="flex items-center gap-2 text-sm text-slate-500">
                <Loader2 className="h-4 w-4 animate-spin" /> Đang tải lịch sử lưu trú
            </div>
        );
    }

    if (bookingsQuery.isError) {
        return (
            <EmptyState title="Không thể tải lịch sử lưu trú" description="Vui lòng thử lại sau." />
        );
    }

    if (bookings.length === 0) {
        return (
            <EmptyState
                title="Chưa có booking lưu trú"
                description="Các yêu cầu đặt phòng của bạn sẽ xuất hiện tại đây."
            />
        );
    }

    return (
        <div className="grid gap-6 xl:grid-cols-[360px_1fr]">
            <Card title="Booking lưu trú">
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
                                        {booking.bookingCode}
                                    </p>
                                </div>
                                <Tag tone={statusTone[booking.statusCode]}>
                                    {booking.statusCode}
                                </Tag>
                            </div>
                            <p className="mt-3 text-xs text-slate-500">
                                {formatDateTime(booking.expectedCheckinAt)} -{" "}
                                {formatDateTime(booking.expectedCheckoutAt)}
                            </p>
                        </button>
                    ))}
                </div>
            </Card>

            {selectedBooking && (
                <div className="space-y-6">
                    <Card
                        title="Chi tiết lưu trú"
                        right={
                            <Tag tone={statusTone[selectedBooking.statusCode]}>
                                {selectedBooking.statusCode}
                            </Tag>
                        }
                    >
                        <div className="grid gap-4 md:grid-cols-2">
                            <SummaryRow label="Thú cưng" value={selectedBooking.petName} />
                            <SummaryRow
                                label="Loại phòng"
                                value={selectedBooking.requestedRoomTypeName}
                            />
                            <SummaryRow
                                label="Phòng cụ thể"
                                value={
                                    selectedBooking.roomCode
                                        ? `${selectedBooking.roomCode} - ${selectedBooking.roomName}`
                                        : "Chờ nhân viên gán phòng"
                                }
                            />
                            <SummaryRow
                                label="Tạm tính"
                                value={formatCurrency(selectedBooking.estimatedPriceVnd)}
                            />
                            <SummaryRow
                                label="Nhận phòng"
                                value={formatDateTime(
                                    selectedBooking.actualCheckinAt ??
                                        selectedBooking.expectedCheckinAt
                                )}
                            />
                            <SummaryRow
                                label="Trả phòng"
                                value={formatDateTime(
                                    selectedBooking.actualCheckoutAt ??
                                        selectedBooking.expectedCheckoutAt
                                )}
                            />
                        </div>
                        {selectedBooking.invoice && (
                            <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-4">
                                <div className="flex items-center gap-2 font-semibold text-amber-900">
                                    <FileText className="h-4 w-4" /> Hóa đơn chờ thanh toán
                                </div>
                                <div className="mt-3 grid gap-3 text-sm md:grid-cols-3">
                                    <SummaryRow
                                        label="Mã HĐ"
                                        value={selectedBooking.invoice.invoiceCode}
                                    />
                                    <SummaryRow
                                        label="Trạng thái"
                                        value={selectedBooking.invoice.statusCode}
                                    />
                                    <SummaryRow
                                        label="Tổng tiền"
                                        value={formatCurrency(
                                            selectedBooking.invoice.totalAmountVnd
                                        )}
                                    />
                                </div>
                            </div>
                        )}
                    </Card>

                    <Card
                        title="Nhật ký chăm sóc"
                        right={<Camera className="h-5 w-5 text-slate-400" />}
                    >
                        {careLogsQuery.isLoading ? (
                            <div className="flex items-center gap-2 text-sm text-slate-500">
                                <Loader2 className="h-4 w-4 animate-spin" /> Đang tải nhật ký
                            </div>
                        ) : careLogsQuery.isError ? (
                            <EmptyState
                                title="Không thể tải nhật ký"
                                description="Vui lòng thử lại sau."
                            />
                        ) : careLogsQuery.data?.length === 0 ? (
                            <EmptyState
                                title="Chưa có nhật ký"
                                description="Nhân viên sẽ cập nhật trong quá trình lưu trú."
                            />
                        ) : (
                            <div className="space-y-4">
                                {careLogsQuery.data?.map((log) => (
                                    <div
                                        key={log.id}
                                        className="rounded-2xl border border-slate-200 p-4"
                                    >
                                        <div className="flex items-start justify-between gap-3">
                                            <div>
                                                <h4 className="font-semibold">
                                                    {new Intl.DateTimeFormat("vi-VN").format(
                                                        new Date(log.logDate)
                                                    )}{" "}
                                                    - {periodLabel[log.periodCode]}
                                                </h4>
                                                <p className="mt-1 text-sm text-slate-500">
                                                    Ăn uống: {log.feedingStatus} | Vệ sinh:{" "}
                                                    {log.hygieneStatus}
                                                </p>
                                                {log.healthNote && (
                                                    <p className="mt-2 text-sm text-slate-600">
                                                        Sức khỏe: {log.healthNote}
                                                    </p>
                                                )}
                                                {log.staffNote && (
                                                    <p className="mt-1 text-sm text-slate-600">
                                                        Ghi chú: {log.staffNote}
                                                    </p>
                                                )}
                                            </div>
                                            <Tag tone="blue">{log.staffName}</Tag>
                                        </div>
                                        {log.media.length > 0 && (
                                            <div className="mt-4 grid gap-3 sm:grid-cols-3">
                                                {log.media.map((media) => (
                                                    <img
                                                        key={media.id}
                                                        src={media.url}
                                                        alt={media.caption || "Ảnh nhật ký chăm sóc"}
                                                        className="h-32 w-full rounded-2xl object-cover"
                                                    />
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </Card>

                    <Button variant="outline" onClick={() => bookingsQuery.refetch()}>
                        Tải lại
                    </Button>
                </div>
            )}
        </div>
    );
  }

