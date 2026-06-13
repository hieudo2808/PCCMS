import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarClock, Loader2, Stethoscope } from "lucide-react";
import { useMemo } from "react";
import { useForm } from "react-hook-form";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button, Input, Textarea } from "~/components/atoms";
import { Card, EmptyState } from "~/components/molecules";
import { appointmentApi } from "~/shared/api/appointmentApi";
import { petApi } from "~/shared/api/petApi";
import { parseApiError } from "~/shared/utils/errorHandlers";
import { clinicTodayIso } from "~/shared/utils/dateGuards";

const medicalBookingSchema = z.object({
    petId: z.string().min(1, "Chọn thú cưng"),
    appointmentDate: z.string().min(1, "Chọn ngày hẹn"),
    slotStart: z.string().min(1, "Chọn giờ hẹn").refine((val) => {
        return val >= "07:00" && val <= "22:00";
    }, "Phòng khám chỉ hoạt động từ 07:00 đến 22:00"),
    requestedVetId: z.string().optional(),
    symptomText: z.string().min(1, "Triệu chứng không được để trống").max(500, "Tối đa 500 ký tự"),
    ownerNote: z.string().max(255, "Ghi chú tối đa 255 ký tự").optional(),
});

const TIME_SLOTS = Array.from({ length: 31 }, (_, i) => {
    const hours = Math.floor(i / 2) + 7;
    const minutes = i % 2 === 0 ? "00" : "30";
    return `${hours.toString().padStart(2, "0")}:${minutes}`;
});

type MedicalBookingFormValues = z.infer<typeof medicalBookingSchema>;

function combineLocalDateTime(date: string, time: string) {
    if (!date || !time) return "";
    return `${date}T${time}`;
}

export function MedicalBookingPage() {
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    const {
        register,
        handleSubmit,
        watch,
        formState: { errors },
    } = useForm<MedicalBookingFormValues>({
        resolver: zodResolver(medicalBookingSchema),
        defaultValues: {
            petId: "",
            appointmentDate: "",
            slotStart: "",
            requestedVetId: "",
            symptomText: "",
            ownerNote: "",
        },
    });

    const appointmentDate = watch("appointmentDate");
    const slotStart = watch("slotStart");
    const todayIso = clinicTodayIso();
    const scheduledStartAt = useMemo(
        () => combineLocalDateTime(appointmentDate, slotStart),
        [appointmentDate, slotStart]
    );

    const petsQuery = useQuery({
        queryKey: ["pets"],
        queryFn: () => petApi.getPets(),
    });

    const vetsQuery = useQuery({
        queryKey: ["available-vets", appointmentDate, slotStart],
        queryFn: () => appointmentApi.listAvailableVets(appointmentDate, slotStart),
        enabled: !!appointmentDate && !!slotStart,
    });

    const createBookingMutation = useMutation({
        mutationFn: (values: MedicalBookingFormValues) =>
            appointmentApi.createMedicalAppointment({
                petId: values.petId,
                appointmentDate: values.appointmentDate,
                slotStart: values.slotStart,
                requestedVetId: values.requestedVetId || undefined,
                symptomText: values.symptomText,
                ownerNote: values.ownerNote,
            }),
        onSuccess: () => {
            toast.success("Đã đặt lịch khám thành công");
            queryClient.invalidateQueries({ queryKey: ["my-appointments"] });
            navigate("/owner/appointments");
        },
        onError: (error) => toast.error(parseApiError(error)),
    });

    const petOptions = petsQuery.data?.content ?? [];
    const isPastTime = scheduledStartAt
        ? new Date(scheduledStartAt).getTime() <= Date.now()
        : false;
    const hasBlockingError = isPastTime;

    return (
        <div className="grid gap-6 xl:grid-cols-[1fr_360px]">
            <Card
                title="Đặt lịch khám bệnh"
                subtitle="Chọn thú cưng, thời gian và bác sĩ khám (tùy chọn). Vui lòng mô tả chi tiết triệu chứng để bác sĩ chuẩn bị tốt nhất."
            >
                {petsQuery.isLoading ? (
                    <div className="flex items-center gap-2 text-sm text-slate-500">
                        <Loader2 className="h-4 w-4 animate-spin" /> Đang tải dữ liệu
                    </div>
                ) : petsQuery.isError ? (
                    <EmptyState
                        title="Không thể tải dữ liệu"
                        description="Vui lòng thử lại sau."
                    />
                ) : petOptions.length === 0 ? (
                    <EmptyState
                        title="Chưa có thú cưng"
                        description="Hãy tạo hồ sơ thú cưng trước khi đặt lịch."
                    />
                ) : (
                    <form
                        className="space-y-5"
                        onSubmit={handleSubmit((values) => createBookingMutation.mutate(values))}
                    >
                        <div className="grid gap-4 md:grid-cols-2">
                            <label className="flex flex-col gap-1.5 text-[13px] font-medium text-slate-700">
                                Thú cưng <span className="text-error-500">*</span>
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
                                Bác sĩ yêu cầu (Tùy chọn)
                                <select
                                    className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20 disabled:bg-slate-50"
                                    {...register("requestedVetId")}
                                    disabled={!appointmentDate || !slotStart || vetsQuery.isLoading}
                                >
                                    <option value="">Không yêu cầu bác sĩ</option>
                                    {vetsQuery.data?.filter(v => v.available).map((vet) => (
                                        <option key={vet.id} value={vet.id}>
                                            {vet.fullName}
                                        </option>
                                    ))}
                                </select>
                                {(!appointmentDate || !slotStart) && (
                                    <span className="text-xs text-slate-500">
                                        Vui lòng chọn ngày giờ để xem danh sách bác sĩ
                                    </span>
                                )}
                            </label>
                        </div>

                        <div className="grid gap-4 md:grid-cols-2">
                            <Input
                                type="date"
                                label="Ngày hẹn *"
                                min={todayIso}
                                error={errors.appointmentDate?.message}
                                {...register("appointmentDate")}
                            />
                            <label className="flex flex-col gap-1.5 text-[13px] font-medium text-slate-700">
                                Giờ hẹn *
                                <select
                                    className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                                    {...register("slotStart")}
                                >
                                    <option value="">Chọn giờ</option>
                                    {TIME_SLOTS.map((time) => (
                                        <option key={time} value={time}>
                                            {time}
                                        </option>
                                    ))}
                                </select>
                                {(errors.slotStart || isPastTime) && (
                                    <span className="text-xs text-error-600">
                                        {errors.slotStart?.message || (isPastTime ? "Thời gian hẹn phải ở tương lai" : "")}
                                    </span>
                                )}
                            </label>
                        </div>

                        <Textarea
                            label="Triệu chứng *"
                            rows={3}
                            placeholder="Mô tả các triệu chứng bất thường của thú cưng (nôn mửa, bỏ ăn, tiêu chảy...)"
                            error={errors.symptomText?.message}
                            {...register("symptomText")}
                        />

                        <Textarea
                            label="Ghi chú thêm (Tùy chọn)"
                            rows={2}
                            placeholder="Ghi chú khác nếu có"
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
                                Xác nhận đặt lịch khám
                            </span>
                        </Button>
                    </form>
                )}
            </Card>

            <Card className="h-fit bg-primary-50/50">
                <div className="flex flex-col items-center p-6 text-center">
                    <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-primary-100 text-primary-600">
                        <Stethoscope className="h-6 w-6" />
                    </div>
                    <h3 className="mb-2 font-semibold text-slate-900">Lưu ý khi khám bệnh</h3>
                    <ul className="space-y-2 text-sm text-slate-600 text-left w-full mt-4">
                        <li className="flex gap-2">
                            <span className="text-primary-500">•</span>
                            <span>Có mặt trước giờ hẹn 10-15 phút để làm thủ tục.</span>
                        </li>
                        <li className="flex gap-2">
                            <span className="text-primary-500">•</span>
                            <span>Nên nhốt thú cưng trong lồng/túi vận chuyển an toàn.</span>
                        </li>
                        <li className="flex gap-2">
                            <span className="text-primary-500">•</span>
                            <span>Mang theo sổ tiêm phòng và lịch sử khám bệnh cũ nếu có.</span>
                        </li>
                    </ul>
                </div>
            </Card>
        </div>
    );
}
