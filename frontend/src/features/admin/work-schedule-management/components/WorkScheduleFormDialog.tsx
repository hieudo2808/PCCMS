import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button, Input } from "~/components/atoms";
import { Card } from "~/components/molecules";
import type {
    WorkSchedule,
    WorkScheduleFormValues,
    WorkScheduleOptions,
    WorkScheduleRole,
    WorkScheduleShift,
    WorkScheduleStatus,
} from "../types";

export const workScheduleSchema = z.object({
    staffId: z.string().min(1, "Vui lòng chọn nhân sự"),
    roleId: z.string().min(1, "Vui lòng chọn vai trò"),
    shiftId: z.string().min(1, "Vui lòng chọn ca làm việc"),
    workDate: z.string().min(1, "Vui lòng nhập ngày làm việc"),
    examRoomId: z.string().optional(),
    stationId: z.string().optional(),
    status: z.enum(["Đã phân công", "Đã hủy", "Đã hoàn thành", ""]).optional(),
    note: z.string().optional(),
    role: z.string().optional(),
    room: z.string().optional(),
    position: z.string().optional(),
    shift: z.string().optional(),
    capacity: z.string().optional(),
});

export type WorkScheduleFormData = z.infer<typeof workScheduleSchema>;

interface WorkScheduleFormDialogProps {
    open: boolean;
    mode: "create" | "edit";
    initialValue: WorkScheduleFormValues;
    options: WorkScheduleOptions;
    loading: boolean;
    error?: string;
    optionError?: string;
    onClose: () => void;
    onSubmit: (data: WorkScheduleFormValues) => void;
    currentSchedule?: WorkSchedule | null;
}

const workScheduleStatuses: WorkScheduleStatus[] = ["Đã phân công", "Đã hủy", "Đã hoàn thành"];

const statusLabels: Record<WorkScheduleStatus, string> = {
    "Đã phân công": "Đã phân công",
    "Đã hủy": "Đã hủy",
    "Đã hoàn thành": "Đã hoàn thành",
};

const roleLabelFromCode = (code?: string): WorkScheduleRole => {
    switch (code) {
        case "VETERINARIAN":
            return "Bác sĩ thú y";
        case "STAFF":
            return "Nhân viên trung tâm";
        default:
            return "Lễ tân";
    }
};

const shiftLabelFromCode = (code?: string, name?: string): WorkScheduleShift => {
    const value = `${code ?? ""} ${name ?? ""}`.toLowerCase();
    if (value.includes("evening") || value.includes("tối") || value.includes("toi")) {
        return "Ca tối";
    }
    if (value.includes("afternoon") || value.includes("chiều") || value.includes("chieu")) {
        return "Ca chiều";
    }
    return "Ca sáng";
};

const formatShiftTime = (startTime?: string, endTime?: string) => {
    if (!startTime && !endTime) return "";
    return ` (${startTime?.slice(0, 5) ?? "--:--"} - ${endTime?.slice(0, 5) ?? "--:--"})`;
};

const locationValue = (type: "exam" | "station", id: string) => `${type}:${id}`;

export function WorkScheduleFormDialog({
    open,
    mode,
    initialValue,
    options,
    loading,
    error,
    optionError,
    onClose,
    onSubmit,
    currentSchedule,
}: WorkScheduleFormDialogProps) {
    const {
        register,
        handleSubmit,
        watch,
        setValue,
        reset,
        formState: { errors },
    } = useForm<WorkScheduleFormData>({
        resolver: zodResolver(workScheduleSchema),
        defaultValues: initialValue as WorkScheduleFormData,
    });

    useEffect(() => {
        if (open) {
            reset(initialValue as WorkScheduleFormData);
        }
    }, [open, initialValue, reset]);

    if (!open) return null;

    const currentStaffId = watch("staffId");
    const currentExamRoomId = watch("examRoomId");
    const currentStationId = watch("stationId");

    const selectedStaff = options.staff.find((staff) => staff.id === currentStaffId) ?? null;
    const missingRequiredOptions =
        options.staff.length === 0 || options.shifts.length === 0 || options.roles.length === 0;
    const canSubmit = !loading && !missingRequiredOptions;

    const selectedLocation = currentExamRoomId
        ? locationValue("exam", currentExamRoomId)
        : currentStationId
          ? locationValue("station", currentStationId)
          : "";

    const onFormSubmit = (data: WorkScheduleFormData) => {
        onSubmit(data as WorkScheduleFormValues);
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4 py-6">
            <div className="w-full max-w-3xl">
                <Card
                    title={mode === "create" ? "Thêm lịch làm việc" : "Sửa lịch làm việc"}
                    subtitle="Chọn dữ liệu nhân sự, ca và vai trò đang có trên hệ thống"
                >
                    <form onSubmit={handleSubmit(onFormSubmit)}>
                        <div className="grid gap-4 md:grid-cols-2">
                            <div className="flex flex-col gap-1.5 md:col-span-2">
                                <label className="text-[13px] font-medium text-slate-700">
                                    Nhân sự <span className="text-rose-500">*</span>
                                </label>
                                <select
                                    className={`h-10 w-full rounded-xl border ${
                                        errors.staffId ? "border-rose-300" : "border-slate-200"
                                    } bg-white px-3 text-[14px] text-slate-900 outline-none transition-all focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20`}
                                    {...register("staffId", {
                                        onChange: (event) => {
                                            const nextStaff = options.staff.find(
                                                (staff) => staff.id === event.target.value
                                            );
                                            if (nextStaff?.roleCode) {
                                                setValue(
                                                    "role",
                                                    roleLabelFromCode(nextStaff.roleCode)
                                                );
                                            }
                                        },
                                    })}
                                >
                                    <option value="">Chọn nhân sự</option>
                                    {options.staff.map((staff) => (
                                        <option key={staff.id} value={staff.id}>
                                            {staff.fullName}
                                            {staff.roleName ? ` - ${staff.roleName}` : ""}
                                        </option>
                                    ))}
                                </select>
                                {errors.staffId && (
                                    <p className="text-xs text-rose-500">
                                        {errors.staffId.message}
                                    </p>
                                )}
                            </div>

                            <div className="flex flex-col gap-1.5">
                                <label className="text-[13px] font-medium text-slate-700">
                                    Vai trò <span className="text-rose-500">*</span>
                                </label>
                                <select
                                    className={`h-10 w-full rounded-xl border ${
                                        errors.roleId ? "border-rose-300" : "border-slate-200"
                                    } bg-white px-3 text-[14px] text-slate-900 outline-none transition-all focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20`}
                                    {...register("roleId", {
                                        onChange: (event) => {
                                            const role = options.roles.find(
                                                (item) => item.id === event.target.value
                                            );
                                            if (role) {
                                                setValue("role", roleLabelFromCode(role.code));
                                            }
                                        },
                                    })}
                                >
                                    <option value="">Chọn vai trò</option>
                                    {options.roles.map((role) => (
                                        <option key={role.id} value={role.id}>
                                            {role.name}
                                        </option>
                                    ))}
                                </select>
                                {errors.roleId && (
                                    <p className="text-xs text-rose-500">{errors.roleId.message}</p>
                                )}
                            </div>

                            <div className="flex flex-col gap-1.5">
                                <label className="text-[13px] font-medium text-slate-700">
                                    Ca làm việc <span className="text-rose-500">*</span>
                                </label>
                                <select
                                    className={`h-10 w-full rounded-xl border ${
                                        errors.shiftId ? "border-rose-300" : "border-slate-200"
                                    } bg-white px-3 text-[14px] text-slate-900 outline-none transition-all focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20`}
                                    {...register("shiftId", {
                                        onChange: (event) => {
                                            const shift = options.shifts.find(
                                                (item) => item.id === event.target.value
                                            );
                                            if (shift) {
                                                setValue(
                                                    "shift",
                                                    shiftLabelFromCode(
                                                        shift.shiftCode,
                                                        shift.shiftName
                                                    )
                                                );
                                            }
                                        },
                                    })}
                                >
                                    <option value="">Chọn ca làm việc</option>
                                    {options.shifts.map((shift) => (
                                        <option key={shift.id} value={shift.id}>
                                            {shift.shiftName || shift.shiftCode}
                                            {formatShiftTime(shift.startTime, shift.endTime)}
                                        </option>
                                    ))}
                                </select>
                                {errors.shiftId && (
                                    <p className="text-xs text-rose-500">
                                        {errors.shiftId.message}
                                    </p>
                                )}
                            </div>

                            <Input
                                label="Phòng làm việc"
                                {...register("room")}
                                readOnly
                                placeholder="Tự động theo vị trí làm việc"
                            />

                            <div className="flex flex-col gap-1.5">
                                <label className="text-[13px] font-medium text-slate-700">
                                    Vị trí làm việc
                                </label>
                                <select
                                    className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-[14px] text-slate-900 outline-none transition-all focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                                    value={selectedLocation}
                                    onChange={(event) => {
                                        const [locationType, locationId] =
                                            event.target.value.split(":");
                                        const room = options.examRooms.find(
                                            (item) => item.id === locationId
                                        );
                                        const station = options.groomingStations.find(
                                            (item) => item.id === locationId
                                        );

                                        setValue(
                                            "examRoomId",
                                            locationType === "exam" ? locationId : ""
                                        );
                                        setValue(
                                            "stationId",
                                            locationType === "station" ? locationId : ""
                                        );
                                        setValue(
                                            "room",
                                            room ? "Khu khám bệnh" : station ? "Khu spa" : ""
                                        );
                                        setValue(
                                            "position",
                                            room
                                                ? `${room.roomCode} - ${room.name}`
                                                : station
                                                  ? `${station.stationCode} - ${station.name}`
                                                  : ""
                                        );
                                    }}
                                >
                                    <option value="">Không chọn vị trí</option>
                                    {options.examRooms.map((room) => (
                                        <option
                                            key={room.id}
                                            value={locationValue("exam", room.id)}
                                        >
                                            Khu khám bệnh - {room.roomCode} - {room.name}
                                        </option>
                                    ))}
                                    {options.groomingStations.map((station) => (
                                        <option
                                            key={station.id}
                                            value={locationValue("station", station.id)}
                                        >
                                            Khu spa - {station.stationCode} - {station.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="flex flex-col gap-1.5">
                                <label className="text-[13px] font-medium text-slate-700">
                                    Ngày làm việc <span className="text-rose-500">*</span>
                                </label>
                                <input
                                    type="date"
                                    className={`h-10 w-full rounded-xl border ${
                                        errors.workDate ? "border-rose-300" : "border-slate-200"
                                    } bg-white px-3 text-[14px] text-slate-900 outline-none transition-all focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20`}
                                    {...register("workDate")}
                                    placeholder="YYYY-MM-DD"
                                />
                                {errors.workDate && (
                                    <p className="text-xs text-rose-500">
                                        {errors.workDate.message}
                                    </p>
                                )}
                            </div>

                            <div className="flex flex-col gap-1.5">
                                <label className="text-[13px] font-medium text-slate-700">
                                    Trạng thái <span className="text-rose-500">*</span>
                                </label>
                                <select
                                    className={`h-10 w-full rounded-xl border ${
                                        errors.status ? "border-rose-300" : "border-slate-200"
                                    } bg-white px-3 text-[14px] text-slate-900 outline-none transition-all focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20`}
                                    {...register("status")}
                                >
                                    <option value="">Chọn trạng thái</option>
                                    {workScheduleStatuses.map((status) => (
                                        <option key={status} value={status}>
                                            {statusLabels[status]}
                                        </option>
                                    ))}
                                </select>
                                {errors.status && (
                                    <p className="text-xs text-rose-500">{errors.status.message}</p>
                                )}
                            </div>

                            <div className="md:col-span-2">
                                <div className="flex flex-col gap-1.5">
                                    <label className="text-[13px] font-medium text-slate-700">
                                        Ghi chú
                                    </label>
                                    <textarea
                                        className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-[14px] text-slate-900 outline-none transition-all focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                                        {...register("note")}
                                        placeholder="Nhập ghi chú nếu cần"
                                        rows={3}
                                    />
                                </div>
                            </div>
                        </div>

                        {selectedStaff &&
                            currentSchedule &&
                            selectedStaff.id !== currentSchedule.staffId && (
                                <p className="mt-3 text-sm text-slate-500">
                                    Nhân sự được chọn: {selectedStaff.fullName}
                                </p>
                            )}

                        {missingRequiredOptions && (
                            <p className="mt-3 text-sm font-medium text-amber-700">
                                Chưa có dữ liệu nhân sự, ca làm việc hoặc vai trò từ hệ thống. Không
                                thể lưu lịch thật.
                            </p>
                        )}

                        {optionError && (
                            <p className="mt-3 text-sm font-medium text-amber-700">{optionError}</p>
                        )}

                        {error && (
                            <p className="mt-3 text-sm font-medium text-error-600">{error}</p>
                        )}

                        <div className="mt-6 flex flex-wrap justify-end gap-3">
                            <Button
                                variant="outline"
                                type="button"
                                onClick={onClose}
                                disabled={loading}
                            >
                                Hủy
                            </Button>
                            <Button type="submit" disabled={!canSubmit}>
                                {loading
                                    ? "Đang lưu..."
                                    : mode === "create"
                                      ? "Thêm lịch làm việc"
                                      : "Cập nhật lịch làm việc"}
                            </Button>
                        </div>
                    </form>
                </Card>
            </div>
        </div>
    );
}
