import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarClock, CheckCircle2, Loader2 } from "lucide-react";
import { useMemo } from "react";
import { useForm } from "react-hook-form";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button, Input, Textarea } from "~/components/atoms";
import { Card, EmptyState, SummaryRow } from "~/components/molecules";
import { boardingApi } from "~/features/boarding/api/boardingApi";
import { petApi } from "~/shared/api/petApi";
import { parseApiError } from "~/shared/utils/errorHandlers";
import { clinicTodayIso } from "~/shared/utils/dateGuards";

function combineLocalDateTime(date: string, time: string) {
    if (!date || !time) return "";
    return `${date}T${time}`;
}

const bookingSchema = z
    .object({
        petId: z.string().min(1, "Chọn thú cưng"),
        roomTypeId: z.string().min(1, "Chọn loại phòng"),
        expectedCheckinDate: z.string().min(1, "Chọn ngày nhận phòng"),
        expectedCheckinTime: z.string().min(1, "Chọn giờ nhận phòng"),
        expectedCheckoutDate: z.string().min(1, "Chọn ngày trả phòng"),
        expectedCheckoutTime: z.string().min(1, "Chọn giờ trả phòng"),
        specialCareRequest: z.string().max(2000).optional(),
    })
    .refine(
        (data) => {
            const checkinAt = combineLocalDateTime(
                data.expectedCheckinDate,
                data.expectedCheckinTime
            );
            const checkoutAt = combineLocalDateTime(
                data.expectedCheckoutDate,
                data.expectedCheckoutTime
            );
            return Boolean(
                checkinAt &&
                checkoutAt &&
                new Date(checkoutAt).getTime() > new Date(checkinAt).getTime()
            );
        },
        {
            message: "Thời gian trả phòng phải sau thời gian nhận phòng",
            path: ["expectedCheckoutTime"],
        }
    );

type BookingFormValues = z.infer<typeof bookingSchema>;

function formatCurrency(value?: number) {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
        value ?? 0
    );
}

function toApiDateTime(date: string, time: string) {
    return new Date(combineLocalDateTime(date, time)).toISOString();
}

function calculateBillableDays(startValue: string, endValue: string) {
    const start = new Date(startValue).getTime();
    const end = new Date(endValue).getTime();
    if (!start || !end || end <= start) return 1;
    return Math.max(1, Math.ceil((end - start) / (24 * 60 * 60 * 1000)));
}

export function UnifiedBookingPage() {
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    const {
        register,
        handleSubmit,
        watch,
        formState: { errors },
    } = useForm<BookingFormValues>({
        resolver: zodResolver(bookingSchema),
        defaultValues: {
            petId: "",
            roomTypeId: "",
            expectedCheckinDate: "",
            expectedCheckinTime: "",
            expectedCheckoutDate: "",
            expectedCheckoutTime: "",
            specialCareRequest: "",
        },
    });

    const checkinDate = watch("expectedCheckinDate");
    const checkinTime = watch("expectedCheckinTime");
    const checkoutDate = watch("expectedCheckoutDate");
    const checkoutTime = watch("expectedCheckoutTime");
    const todayIso = clinicTodayIso();
    const checkinAt = useMemo(
        () => combineLocalDateTime(checkinDate, checkinTime),
        [checkinDate, checkinTime]
    );
    const checkoutAt = useMemo(
        () => combineLocalDateTime(checkoutDate, checkoutTime),
        [checkoutDate, checkoutTime]
    );
    const selectedRoomTypeId = watch("roomTypeId");
    const canCheckAvailability = Boolean(
        checkinAt && checkoutAt && new Date(checkoutAt).getTime() > new Date(checkinAt).getTime()
    );

    const petsQuery = useQuery({
        queryKey: ["pets"],
        queryFn: () => petApi.getPets(),
    });

    const availabilityQuery = useQuery({
        queryKey: ["boarding-availability", checkinAt, checkoutAt],
        queryFn: () =>
            boardingApi.getAvailability(
                toApiDateTime(checkinDate, checkinTime),
                toApiDateTime(checkoutDate, checkoutTime)
            ),
        enabled: canCheckAvailability,
    });

    const createBookingMutation = useMutation({
        mutationFn: (values: BookingFormValues) =>
            boardingApi.createBooking({
                petId: values.petId,
                roomTypeId: values.roomTypeId,
                expectedCheckinAt: toApiDateTime(
                    values.expectedCheckinDate,
                    values.expectedCheckinTime
                ),
                expectedCheckoutAt: toApiDateTime(
                    values.expectedCheckoutDate,
                    values.expectedCheckoutTime
                ),
                specialCareRequest: values.specialCareRequest,
            }),
        onSuccess: () => {
            toast.success("Đã gửi yêu cầu đặt phòng");
            queryClient.invalidateQueries({ queryKey: ["my-boarding-bookings"] });
            navigate("/owner/boarding/tracking");
        },
        onError: (error) => toast.error(parseApiError(error)),
    });

    const selectedAvailability = availabilityQuery.data?.find(
        (item) => item.roomTypeId === selectedRoomTypeId
    );
    const billableDays = useMemo(
        () => calculateBillableDays(checkinAt, checkoutAt),
        [checkinAt, checkoutAt]
    );
    const estimatedPrice = (selectedAvailability?.baseDailyPriceVnd ?? 0) * billableDays;
    const petOptions = petsQuery.data?.content ?? [];




    return (
        <div className="grid gap-6 xl:grid-cols-[1fr_360px]">
            <Card
                title="Đặt phòng lưu trú"
                subtitle="Chọn thú cưng, khoảng thời gian và loại phòng. Nhân viên sẽ duyệt và gán phòng cụ thể."
            >
                {petsQuery.isLoading ? (
                    <div className="flex items-center gap-2 text-sm text-slate-500">
                        <Loader2 className="h-4 w-4 animate-spin" /> Đang tải danh sách thú cưng
                    </div>
                ) : petsQuery.isError ? (
                    <EmptyState
                        title="Không thể tải hồ sơ thú cưng"
                        description="Vui lòng thử lại sau."
                    />
                ) : petOptions.length === 0 ? (
                    <EmptyState
                        title="Chưa có thú cưng"
                        description="Hãy tạo hồ sơ thú cưng trước khi đặt phòng."
                    />
                ) : (
                    <form
                        className="space-y-5"
                        onSubmit={handleSubmit((values) => createBookingMutation.mutate(values))}
                    >
                        <div className="grid gap-4 md:grid-cols-2">
                            <label className="flex flex-col gap-1.5 text-[13px] font-medium text-slate-700">
                                Thú cưng
                                <select
                                    className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                                    {...register("petId")}
                                >
                                    <option value="">Chọn thú cưng</option>
                                    {petOptions.map((pet) => (
                                        <option key={pet.id} value={pet.id}>
                                            {pet.name}
                                        </option>
                                    ))}
                                </select>
                                {errors.petId && (
                                    <span className="text-xs text-error-600">
                                        {errors.petId.message}
                                    </span>
                                )}
                            </label>
                        </div>

                        <div className="grid gap-4 md:grid-cols-2">
                            <Input
                                type="date"
                                label="Ngày nhận phòng"
                                min={todayIso}
                                error={errors.expectedCheckinDate?.message}
                                {...register("expectedCheckinDate")}
                            />
                            <Input
                                type="time"
                                label="Giờ nhận phòng"
                                error={errors.expectedCheckinTime?.message}
                                {...register("expectedCheckinTime")}
                            />
                            <Input
                                type="date"
                                label="Ngày trả phòng"
                                min={checkinDate || todayIso}
                                error={errors.expectedCheckoutDate?.message}
                                {...register("expectedCheckoutDate")}
                            />
                            <Input
                                type="time"
                                label="Giờ trả phòng"
                                error={errors.expectedCheckoutTime?.message}
                                {...register("expectedCheckoutTime")}
                            />
                        </div>

                        <div className="grid gap-4 md:grid-cols-2">
                            <label className="flex flex-col gap-1.5 text-[13px] font-medium text-slate-700">
                                Loại phòng
                                <select
                                    className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                                    disabled={
                                        !canCheckAvailability ||
                                        availabilityQuery.isLoading ||
                                        availabilityQuery.isError
                                    }
                                    {...register("roomTypeId")}
                                >
                                    <option value="">
                                        {!canCheckAvailability
                                            ? "Chọn ngày giờ nhận/trả phòng trước"
                                            : availabilityQuery.isLoading
                                              ? "Đang kiểm tra phòng trống"
                                              : availabilityQuery.isError
                                                ? "Không thể kiểm tra phòng trống"
                                                : "Chọn loại phòng"}
                                    </option>
                                    {availabilityQuery.data?.map((item) => (
                                        <option
                                            key={item.roomTypeId}
                                            value={item.roomTypeId}
                                            disabled={item.availableRooms === 0}
                                        >
                                            {item.roomTypeName} -{" "}
                                            {formatCurrency(item.baseDailyPriceVnd)}/ngày - còn{" "}
                                            {item.availableRooms}
                                        </option>
                                    ))}
                                </select>
                                {errors.roomTypeId && (
                                    <span className="text-xs text-error-600">
                                        {errors.roomTypeId.message}
                                    </span>
                                )}
                            </label>
                        </div>

                        {canCheckAvailability && (
                            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm">
                                {availabilityQuery.isLoading ? (
                                    <span className="inline-flex items-center gap-2 text-slate-500">
                                        <Loader2 className="h-4 w-4 animate-spin" /> Đang kiểm tra
                                        phòng trống
                                    </span>
                                ) : availabilityQuery.isError ? (
                                    <span className="text-error-600">
                                        Không thể kiểm tra phòng trống
                                    </span>
                                ) : (
                                    <span className="text-slate-600">
                                        Chọn loại phòng để xem giá tạm tính. Hệ thống tính tối thiểu
                                        1 ngày và làm tròn lên theo chu kỳ 24 giờ.
                                    </span>
                                )}
                            </div>
                        )}

                        <Textarea
                            label="Yêu cầu chăm sóc đặc biệt"
                            rows={4}
                            placeholder="Chế độ ăn, thuốc, thói quen, lưu ý hạn chế..."
                            error={errors.specialCareRequest?.message}
                            {...register("specialCareRequest")}
                        />

                        <Button
                            type="submit"
                            disabled={
                                createBookingMutation.isPending ||
                                !selectedAvailability ||
                                selectedAvailability.availableRooms === 0
                            }
                        >
                            <span className="inline-flex items-center gap-2">
                                {createBookingMutation.isPending ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                    <CalendarClock className="h-4 w-4" />
                                )}
                                Gửi yêu cầu đặt phòng
                            </span>
                        </Button>
                    </form>
                )}
            </Card>

            <Card title="Tạm tính chi phí">
                <div className="space-y-3">
                    <SummaryRow label="Số ngày tính tiền" value={`${billableDays} ngày`} />
                    <SummaryRow
                        label="Giá phòng/ngày"
                        value={formatCurrency(selectedAvailability?.baseDailyPriceVnd)}
                    />
                    <SummaryRow
                        label="Tổng tạm tính"
                        value={
                            <span className="text-emerald-700">
                                {formatCurrency(estimatedPrice)}
                            </span>
                        }
                    />
                    <div className="rounded-2xl bg-emerald-50 p-4 text-sm text-emerald-900">
                        <CheckCircle2 className="mb-2 h-5 w-5" />
                        Hóa đơn chính thức sẽ được tạo khi nhân viên check-out và xác nhận thời gian
                        lưu trú thực tế.
                    </div>
                </div>
            </Card>
        </div>

    );

}
