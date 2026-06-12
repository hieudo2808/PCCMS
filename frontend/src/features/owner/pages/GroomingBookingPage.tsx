import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarClock, CheckCircle2, Loader2, Sparkles } from "lucide-react";
import { useMemo } from "react";
import { useForm } from "react-hook-form";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button, Input, Textarea } from "~/components/atoms";
import { Card, EmptyState, SummaryRow } from "~/components/molecules";
import { groomingApi } from "~/features/grooming/api/groomingApi";
import { petApi } from "~/shared/api/petApi";
import { parseApiError } from "~/shared/utils/errorHandlers";
import { clinicTodayIso } from "~/shared/utils/dateGuards";

const groomingBookingSchema = z.object({
    petId: z.string().min(1, "Chọn thú cưng"),
    serviceId: z.string().min(1, "Chọn dịch vụ làm đẹp"),
    scheduledDate: z.string().min(1, "Chọn ngày hẹn"),
    scheduledTime: z.string().min(1, "Chọn giờ hẹn"),
    ownerNote: z.string().max(2000, "Ghi chú tối đa 2000 ký tự").optional(),
});

type GroomingBookingFormValues = z.infer<typeof groomingBookingSchema>;

function formatCurrency(value?: number) {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
        value ?? 0
    );
}

function formatDuration(minutes?: number) {
    if (!minutes) return "0 phút";
    const hours = Math.floor(minutes / 60);
    const remain = minutes % 60;
    if (hours === 0) return `${remain} phút`;
    if (remain === 0) return `${hours} giờ`;
    return `${hours} giờ ${remain} phút`;
}

function combineLocalDateTime(date: string, time: string) {
    if (!date || !time) return "";
    return `${date}T${time}`;
}

function toApiDateTime(date: string, time: string) {
    return new Date(combineLocalDateTime(date, time)).toISOString();
}

function addMinutes(value: string, minutes?: number) {
    if (!value || !minutes) return "";
    return new Date(new Date(value).getTime() + minutes * 60 * 1000).toLocaleString("vi-VN");
}

export function GroomingBookingPage() {
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    const {
        register,
        handleSubmit,
        watch,
        formState: { errors },
    } = useForm<GroomingBookingFormValues>({
        resolver: zodResolver(groomingBookingSchema),
        defaultValues: {
            petId: "",
            serviceId: "",
            scheduledDate: "",
            scheduledTime: "",
            ownerNote: "",
        },
    });

    const selectedServiceId = watch("serviceId");
    const scheduledDate = watch("scheduledDate");
    const scheduledTime = watch("scheduledTime");
    const todayIso = clinicTodayIso();
    const scheduledStartAt = useMemo(
        () => combineLocalDateTime(scheduledDate, scheduledTime),
        [scheduledDate, scheduledTime]
    );

    const petsQuery = useQuery({
        queryKey: ["pets"],
        queryFn: () => petApi.getPets(),
    });

    const servicesQuery = useQuery({
        queryKey: ["grooming-services"],
        queryFn: () => groomingApi.getServices(),
    });

    const createBookingMutation = useMutation({
        mutationFn: (values: GroomingBookingFormValues) =>
            groomingApi.createBooking({
                petId: values.petId,
                serviceId: values.serviceId,
                scheduledStartAt: toApiDateTime(values.scheduledDate, values.scheduledTime),
                ownerNote: values.ownerNote,
            }),
        onSuccess: () => {
            toast.success("Đã gửi yêu cầu làm đẹp");
            queryClient.invalidateQueries({ queryKey: ["my-grooming-tickets"] });
            navigate("/owner/grooming/tracking");
        },
        onError: (error) => toast.error(parseApiError(error)),
    });

    const selectedService = servicesQuery.data?.find((service) => service.id === selectedServiceId);
    const petOptions = petsQuery.data?.content ?? [];
    const scheduledEnd = useMemo(
        () => addMinutes(scheduledStartAt, selectedService?.durationMinutes),
        [scheduledStartAt, selectedService?.durationMinutes]
    );
    const isPastTime = scheduledStartAt
        ? new Date(scheduledStartAt).getTime() <= Date.now()
        : false;
    const hasBlockingError = !selectedService || isPastTime;

    return (
        <div className="grid gap-6 xl:grid-cols-[1fr_360px]">
            <Card
                title="Đăng ký dịch vụ làm đẹp"
                subtitle="Chọn thú cưng, gói dịch vụ và giờ hẹn. Nhân viên sẽ duyệt lịch và sắp xếp khu làm đẹp phù hợp."
            >
                {petsQuery.isLoading || servicesQuery.isLoading ? (
                    <div className="flex items-center gap-2 text-sm text-slate-500">
                        <Loader2 className="h-4 w-4 animate-spin" /> Đang tải dữ liệu
                    </div>
                ) : petsQuery.isError || servicesQuery.isError ? (
                    <EmptyState
                        title="Không thể tải dữ liệu đặt lịch"
                        description="Vui lòng thử lại sau."
                    />
                ) : petOptions.length === 0 ? (
                    <EmptyState
                        title="Chưa có thú cưng"
                        description="Hãy tạo hồ sơ thú cưng trước khi đặt lịch làm đẹp."
                    />
                ) : servicesQuery.data?.length === 0 ? (
                    <EmptyState
                        title="Chưa có dịch vụ làm đẹp"
                        description="Vui lòng liên hệ trung tâm để được hỗ trợ."
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

                            <label className="flex flex-col gap-1.5 text-[13px] font-medium text-slate-700">
                                Dịch vụ làm đẹp
                                <select
                                    className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                                    {...register("serviceId")}
                                >
                                    <option value="">Chọn dịch vụ</option>
                                    {servicesQuery.data?.map((service) => (
                                        <option key={service.id} value={service.id}>
                                            {service.name} - {formatCurrency(service.basePriceVnd)}
                                        </option>
                                    ))}
                                </select>
                                {errors.serviceId && (
                                    <span className="text-xs text-error-600">
                                        {errors.serviceId.message}
                                    </span>
                                )}
                            </label>
                        </div>

                        <div className="grid gap-4 md:grid-cols-2">
                            <Input
                                type="date"
                                label="Ngày hẹn"
                                min={todayIso}
                                error={errors.scheduledDate?.message}
                                {...register("scheduledDate")}
                            />
                            <Input
                                type="time"
                                label="Giờ hẹn"
                                error={
                                    errors.scheduledTime?.message ||
                                    (isPastTime ? "Thời gian hẹn phải ở tương lai" : undefined)
                                }
                                {...register("scheduledTime")}
                            />
                        </div>

                        <Textarea
                            label="Ghi chú cho nhân viên"
                            rows={4}
                            placeholder="Ví dụ: bé sợ máy sấy, cần cắt móng nhẹ, lông bị rối..."
                            error={errors.ownerNote?.message}
                            {...register("ownerNote")}
                        />

                        <Button
                            type="submit"
                            disabled={createBookingMutation.isPending || hasBlockingError}
                        >
                            <span className="inline-flex items-center gap-2">
                                {createBookingMutation.isPending ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                    <CalendarClock className="h-4 w-4" />
                                )}
                                Gửi yêu cầu làm đẹp
                            </span>
                        </Button>
                    </form>
                )}
            </Card>

            <Card title="Tạm tính lịch hẹn">
                <div className="space-y-3">
                    <SummaryRow label="Dịch vụ" value={selectedService?.name ?? "Chưa chọn"} />
                    <SummaryRow
                        label="Thời lượng"
                        value={formatDuration(selectedService?.durationMinutes)}
                    />
                    <SummaryRow label="Kết thúc dự kiến" value={scheduledEnd || "Chưa có"} />
                    <SummaryRow
                        label="Số tiền dự kiến"
                        value={
                            <span className="text-emerald-700">
                                {formatCurrency(selectedService?.basePriceVnd)}
                            </span>
                        }
                    />
                    <div className="rounded-2xl bg-emerald-50 p-4 text-sm text-emerald-900">
                        {selectedService ? (
                            <CheckCircle2 className="mb-2 h-5 w-5" />
                        ) : (
                            <Sparkles className="mb-2 h-5 w-5" />
                        )}
                        Hóa đơn chính thức sẽ được tạo khi nhân viên hoàn thành dịch vụ làm đẹp.
                    </div>
                </div>
            </Card>
        </div>
    );
}
