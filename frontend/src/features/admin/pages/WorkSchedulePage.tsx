import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Button, Tag, AutocompleteInput } from "~/components/atoms";
import { Card } from "~/components/molecules";
import { WorkScheduleFormDialog } from "../work-schedule-management/components/WorkScheduleFormDialog";
import { WorkScheduleCancelDialog } from "../work-schedule-management/components/WorkScheduleCancelDialog";
import {
    applyWeeklySchedulePlan,
    cancelWorkSchedule,
    createWorkSchedule,
    getWorkScheduleOptions,
    getWorkSchedules,
    previewWeeklySchedulePlan,
    searchWorkSchedules,
    updateWorkSchedule,
    roleFromBackend,
} from "../work-schedule-management/workScheduleService";
import type {
    WorkSchedule,
    WorkScheduleFormValues,
    WorkScheduleOptions,
    WorkScheduleSearchParams,
    WeeklySchedulePlanResponse,
} from "../work-schedule-management/types";

const emptyFilters: WorkScheduleSearchParams = {
    keyword: "",
    role: "",
    room: "",
    workDate: "",
    shift: "",
    status: "",
};

const emptyForm: WorkScheduleFormValues = {
    staffId: "",
    shiftId: "",
    roleId: "",
    examRoomId: "",
    stationId: "",
    capacity: "1",
    role: "",
    room: "",
    workDate: "",
    shift: "",
    status: "",
    note: "",
};

const emptyOptions: WorkScheduleOptions = {
    staff: [],
    shifts: [],
    roles: [],
    examRooms: [],
    groomingStations: [],
};

const shiftOrder: WorkSchedule["shift"][] = ["Ca sáng", "Ca chiều", "Ca tối"];

const weekdayLabels = ["CN", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"];

const shiftOptions = ["Ca sáng", "Ca chiều", "Ca tối"];

const statusOptions = ["Đã phân công", "Đã hủy"];

const scheduleWeekStorageKey = "pccms-admin-schedule-week-anchor";

const parseDateValue = (value?: string | null) => {
    if (!value) return null;

    const parsedDate = new Date(`${value}T00:00:00`);
    return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
};

const getDefaultWeekDate = () => {
    if (typeof window === "undefined") {
        return new Date();
    }

    return parseDateValue(window.localStorage.getItem(scheduleWeekStorageKey)) ?? new Date();
};

const buildFormValues = (schedule: WorkSchedule): WorkScheduleFormValues => ({
    staffId: schedule.staffId,
    shiftId: schedule.shiftId ?? "",
    roleId: schedule.roleId ?? "",
    examRoomId: schedule.examRoomId ?? "",
    stationId: schedule.stationId ?? "",
    capacity: String(schedule.capacity ?? 1),
    role: schedule.role,
    room: schedule.room,
    workDate: schedule.workDate,
    shift: schedule.shift,
    status: schedule.status,
    note: schedule.note,
});

const getTextValue = (record: Record<string, unknown>, keys: string[]) => {
    for (const key of keys) {
        const value = record[key];

        if (typeof value === "string" && value.trim()) {
            return value.trim();
        }
    }

    return "";
};

const getScheduleDetails = (schedule: WorkSchedule, options?: WorkScheduleOptions) => {
    if (schedule.examRoomId && options) {
        const room = options.examRooms.find((item) => item.id === schedule.examRoomId);

        if (room) {
            return {
                room: "Khu khám bệnh",
                position: `${room.roomCode} - ${room.name}`,
            };
        }
    }

    if (schedule.stationId && options) {
        const station = options.groomingStations.find((item) => item.id === schedule.stationId);

        if (station) {
            return {
                room: "Khu spa",
                position: `${station.stationCode} - ${station.name}`,
            };
        }
    }

    const record = schedule as unknown as Record<string, unknown>;

    const room =
        getTextValue(record, [
            "room",
            "roomName",
            "roomCode",
            "department",
            "location",
            "workRoom",
        ]) || "Chưa có phòng";

    const position =
        getTextValue(record, [
            "position",
            "positionName",
            "jobPosition",
            "workPosition",
            "dutyPosition",
        ]) || "Chưa có vị trí";

    return {
        room,
        position,
    };
};

const normalizeDateValue = (value?: string) => {
    if (!value) return "";

    const trimmedValue = value.trim();

    if (!trimmedValue) return "";

    if (trimmedValue.includes("/")) {
        const parts = trimmedValue.split("/");

        if (parts.length !== 3) {
            return trimmedValue;
        }

        const [day, month, year] = parts;

        return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
    }

    return trimmedValue;
};

const formatISODate = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
};

const formatShortDate = (date: Date) => {
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");

    return `${day}/${month}`;
};

const formatFullDate = (date: Date) => {
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = date.getFullYear();

    return `${day}/${month}/${year}`;
};

const getStartOfWeek = (date: Date) => {
    const clone = new Date(date);

    clone.setHours(0, 0, 0, 0);

    const day = clone.getDay();

    clone.setDate(clone.getDate() - day);

    return clone;
};

const addDays = (date: Date, days: number) => {
    const clone = new Date(date);

    clone.setDate(clone.getDate() + days);

    return clone;
};

const getWeekRange = (startDate: Date) => ({
    fromDate: formatISODate(startDate),
    toDate: formatISODate(addDays(startDate, 6)),
});

const formatWeekRange = (startDate: Date) => {
    const endDate = addDays(startDate, 6);

    return `${formatFullDate(startDate)} - ${formatFullDate(endDate)}`;
};

const formatDayHeading = (date: Date) => {
    const weekday = weekdayLabels[date.getDay()];

    return `${weekday}, ${formatShortDate(date)}`;
};

const inputClassName =
    "h-11 w-full min-w-0 rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-50";

const selectClassName =
    "h-11 w-full min-w-0 rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-50";

export function WorkSchedulePage() {
    const [schedules, setSchedules] = useState<WorkSchedule[]>([]);
    const [visibleSchedules, setVisibleSchedules] = useState<WorkSchedule[]>([]);
    const [filters, setFilters] = useState<WorkScheduleSearchParams>(emptyFilters);
    const [loading, setLoading] = useState(true);
    const [searchError, setSearchError] = useState("");
    const [feedback, setFeedback] = useState("");
    const [formOpen, setFormOpen] = useState(false);
    const [formMode, setFormMode] = useState<"create" | "edit">("create");
    const [formValue, setFormValue] = useState<WorkScheduleFormValues>(emptyForm);
    const [formLoading, setFormLoading] = useState(false);
    const [formError, setFormError] = useState("");
    const { data: scheduleOptions = emptyOptions, isError: isOptionError } = useQuery({
        queryKey: ["workScheduleOptions"],
        queryFn: getWorkScheduleOptions,
    });
    const optionError = isOptionError
        ? "Không tải được dữ liệu nhân sự, ca làm việc hoặc vai trò từ hệ thống."
        : "";
    const [editingSchedule, setEditingSchedule] = useState<WorkSchedule | null>(null);
    const [cancelTarget, setCancelTarget] = useState<WorkSchedule | null>(null);
    const [cancelLoading, setCancelLoading] = useState(false);
    const [cancelError, setCancelError] = useState("");

    const [weekAnchor, setWeekAnchor] = useState(() => getStartOfWeek(getDefaultWeekDate()));
    const [detailSlot, setDetailSlot] = useState<{
        date: Date;
        shift: WorkSchedule["shift"];
    } | null>(null);
    const [detailSearch, setDetailSearch] = useState("");
    const [planSourceWeek, setPlanSourceWeek] = useState(() =>
        formatISODate(addDays(weekAnchor, -7))
    );
    const [planTargetWeek, setPlanTargetWeek] = useState(() => formatISODate(weekAnchor));
    const [planRoleId, setPlanRoleId] = useState("");
    const [planShiftId, setPlanShiftId] = useState("");
    const [planPreview, setPlanPreview] = useState<WeeklySchedulePlanResponse | null>(null);
    const [planLoading, setPlanLoading] = useState(false);
    const [planError, setPlanError] = useState("");

    const persistWeekAnchor = (startDate: Date) => {
        if (typeof window !== "undefined") {
            window.localStorage.setItem(scheduleWeekStorageKey, formatISODate(startDate));
        }
    };

    const loadSchedules = async (startDate = weekAnchor) => {
        setLoading(true);

        try {
            const data = await getWorkSchedules(getWeekRange(startDate));
            setSchedules(data);
            setVisibleSchedules(data);
            setSearchError("");
            persistWeekAnchor(startDate);
        } catch {
            setSchedules([]);
            setVisibleSchedules([]);
            setSearchError("Không tải được lịch làm việc từ hệ thống.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        void loadSchedules();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const total = schedules.length;
    const assigned = schedules.filter((schedule) => schedule.status === "Đã phân công").length;
    const cancelled = schedules.filter((schedule) => schedule.status === "Đã hủy").length;
    const staffCount = new Set(schedules.map((schedule) => schedule.staffId)).size;
    const filteredSchedules = useMemo(() => visibleSchedules, [visibleSchedules]);

    const visibleWeekDates = useMemo(() => {
        return Array.from({ length: 7 }, (_, index) => addDays(weekAnchor, index));
    }, [weekAnchor]);

    const groupedByDate = useMemo(() => {
        return visibleWeekDates.map((date) => {
            const dateKey = formatISODate(date);
            const items = filteredSchedules.filter(
                (schedule) => normalizeDateValue(schedule.workDate) === dateKey
            );

            return {
                date,
                shifts: shiftOrder.map((shift) => ({
                    shift,
                    items: items.filter((item) => item.shift === shift),
                })),
            };
        });
    }, [filteredSchedules, visibleWeekDates]);

    const detailSlotItems = useMemo(() => {
        if (!detailSlot) return [];

        const dateKey = formatISODate(detailSlot.date);
        const keyword = detailSearch.trim().toLowerCase();

        return filteredSchedules.filter((schedule) => {
            const matchesSlot =
                normalizeDateValue(schedule.workDate) === dateKey &&
                schedule.shift === detailSlot.shift;

            if (!matchesSlot) {
                return false;
            }

            if (!keyword) {
                return true;
            }

            return [
                schedule.staffName,
                schedule.role,
                schedule.room,
                schedule.position,
                schedule.note,
            ]
                .filter(Boolean)
                .some((value) => value.toLowerCase().includes(keyword));
        });
    }, [detailSearch, detailSlot, filteredSchedules]);

    const updateFilter = (name: keyof WorkScheduleSearchParams, value: string) => {
        setFilters((current) => ({
            ...current,
            [name]: value,
        }));
    };

    const runSearch = async () => {
        const hasCriteria =
            filters.keyword.trim() ||
            filters.role ||
            filters.room.trim() ||
            filters.workDate.trim() ||
            filters.shift ||
            filters.status;

        if (!hasCriteria) {
            setSearchError("Cần nhập ít nhất một tiêu chí tìm kiếm");
            return;
        }

        setSearchError("");
        setFeedback("");
        setLoading(true);

        try {
            const result = await searchWorkSchedules(filters);

            setVisibleSchedules(result);

            if (filters.workDate.trim()) {
                const normalizedDate = normalizeDateValue(filters.workDate);
                const parsedDate = parseDateValue(normalizedDate);

                if (parsedDate) {
                    const searchWeek = getStartOfWeek(parsedDate);
                    setWeekAnchor(searchWeek);
                    const weekData = await getWorkSchedules(getWeekRange(searchWeek));
                    setSchedules(weekData);
                    persistWeekAnchor(searchWeek);
                }
            }

            if (result.length === 0) {
                setFeedback("Không tìm thấy lịch làm việc nào thoả mãn tiêu chí tìm kiếm");
            }
        } catch {
            setVisibleSchedules([]);
            setSearchError("Không tìm kiếm được lịch làm việc từ hệ thống.");
        } finally {
            setLoading(false);
        }
    };

    const resetFilters = async () => {
        setFilters(emptyFilters);
        setSearchError("");
        setFeedback("");
        setDetailSlot(null);
        const currentWeek = getStartOfWeek(getDefaultWeekDate());
        setWeekAnchor(currentWeek);

        await loadSchedules(currentWeek);
    };

    const openCreate = () => {
        setFormMode("create");
        setFormValue(emptyForm);
        setEditingSchedule(null);
        setFormError("");
        setFormOpen(true);
    };

    const openCreateForSlot = (date: Date, shift: WorkSchedule["shift"]) => {
        const shiftOption = scheduleOptions.shifts.find((option) => {
            const value = `${option.shiftCode} ${option.shiftName}`.toLowerCase();
            if (shift === "Ca tối")
                return value.includes("evening") || value.includes("tối") || value.includes("toi");
            if (shift === "Ca chiều")
                return (
                    value.includes("afternoon") ||
                    value.includes("chiều") ||
                    value.includes("chieu")
                );
            return value.includes("morning") || value.includes("sáng") || value.includes("sang");
        });

        setFormMode("create");
        setFormValue({
            ...emptyForm,
            workDate: formatFullDate(date),
            shift,
            shiftId: shiftOption?.id ?? "",
            status: "Đã phân công",
        });
        setEditingSchedule(null);
        setFormError("");
        setFormOpen(true);
    };

    const openEdit = (schedule: WorkSchedule) => {
        setDetailSlot(null);
        setFormMode("edit");
        setEditingSchedule(schedule);
        setFormValue(buildFormValues(schedule));
        setFormError("");
        setFormOpen(true);
    };

    const submitForm = async (formData: WorkScheduleFormValues) => {
        setFormLoading(true);
        setFormError("");

        try {
            if (
                scheduleOptions.staff.length === 0 ||
                scheduleOptions.shifts.length === 0 ||
                scheduleOptions.roles.length === 0
            ) {
                throw new Error("Chưa có dữ liệu nhân sự, ca làm việc hoặc vai trò từ hệ thống");
            }

            const saved =
                formMode === "create"
                    ? await createWorkSchedule(formData)
                    : await updateWorkSchedule(editingSchedule?.id ?? "", formData);

            setFormOpen(false);
            setFeedback("Đã lưu lịch làm việc trên hệ thống.");

            const savedDate = parseDateValue(normalizeDateValue(saved.workDate));

            if (savedDate) {
                const savedWeek = getStartOfWeek(savedDate);
                setWeekAnchor(savedWeek);
                await loadSchedules(savedWeek);
            } else {
                await loadSchedules();
            }
        } catch (error) {
            setFormError(error instanceof Error ? error.message : "Đã xảy ra lỗi");
        } finally {
            setFormLoading(false);
        }
    };

    const openCancel = (schedule: WorkSchedule) => {
        setCancelTarget(schedule);
        setCancelError("");
    };

    const goToPreviousWeek = () => {
        setDetailSlot(null);
        const nextWeek = addDays(weekAnchor, -7);
        setWeekAnchor(nextWeek);
        void loadSchedules(nextWeek);
    };

    const goToNextWeek = () => {
        setDetailSlot(null);
        const nextWeek = addDays(weekAnchor, 7);
        setWeekAnchor(nextWeek);
        void loadSchedules(nextWeek);
    };

    const goToCurrentWeek = () => {
        setDetailSlot(null);
        const currentWeek = getStartOfWeek(getDefaultWeekDate());
        setWeekAnchor(currentWeek);
        void loadSchedules(currentWeek);
    };

    const goToSelectedWeek = (value: string) => {
        if (!value) return;

        const selectedDate = new Date(`${value}T00:00:00`);

        if (Number.isNaN(selectedDate.getTime())) return;

        setDetailSlot(null);
        const selectedWeek = getStartOfWeek(selectedDate);
        setWeekAnchor(selectedWeek);
        setPlanTargetWeek(formatISODate(selectedWeek));
        void loadSchedules(selectedWeek);
    };

    const buildPlanPayload = () => ({
        sourceWeekStart: formatISODate(getStartOfWeek(new Date(`${planSourceWeek}T00:00:00`))),
        targetWeekStart: formatISODate(getStartOfWeek(new Date(`${planTargetWeek}T00:00:00`))),
        roleIds: planRoleId ? [planRoleId] : [],
        shiftIds: planShiftId ? [planShiftId] : [],
    });

    const previewPlan = async () => {
        setPlanLoading(true);
        setPlanError("");
        setFeedback("");

        try {
            const result = await previewWeeklySchedulePlan(buildPlanPayload());
            setPlanPreview(result);
            if (result.items.length === 0) {
                setPlanError("Không có lịch phù hợp trong tuần nguồn.");
            }
        } catch (error) {
            setPlanPreview(null);
            setPlanError(error instanceof Error ? error.message : "Không thể preview lịch tuần");
        } finally {
            setPlanLoading(false);
        }
    };

    const applyPlan = async () => {
        setPlanLoading(true);
        setPlanError("");

        try {
            const result = await applyWeeklySchedulePlan(buildPlanPayload());
            setPlanPreview(result);
            setFeedback(
                `Đã tạo ${result.createdCount} lịch, bỏ qua ${result.skippedCount} lịch trùng.`
            );
            const targetWeek = getStartOfWeek(new Date(`${planTargetWeek}T00:00:00`));
            setWeekAnchor(targetWeek);
            await loadSchedules(targetWeek);
        } catch (error) {
            setPlanError(error instanceof Error ? error.message : "Không thể áp dụng lịch tuần");
        } finally {
            setPlanLoading(false);
        }
    };

    const confirmCancel = async () => {
        if (!cancelTarget) return;

        setCancelLoading(true);
        setCancelError("");

        try {
            const updated = await cancelWorkSchedule(cancelTarget.id);

            setCancelTarget(null);
            setFeedback("Đã hủy lịch làm việc trên hệ thống.");

            const updatedDate = parseDateValue(normalizeDateValue(updated.workDate));
            if (updatedDate) {
                const updatedWeek = getStartOfWeek(updatedDate);
                setWeekAnchor(updatedWeek);
                await loadSchedules(updatedWeek);
            } else {
                await loadSchedules();
            }
        } catch (error) {
            setCancelError(error instanceof Error ? error.message : "Không thể hủy lịch làm việc");
        } finally {
            setCancelLoading(false);
        }
    };

    const renderSchedulePreview = (schedule: WorkSchedule) => {
        const scheduleDetails = getScheduleDetails(schedule, scheduleOptions);

        return (
            <div
                key={schedule.id}
                className={`rounded-2xl border px-3 py-2 text-xs ${
                    schedule.status === "Đã hủy"
                        ? "border-amber-200 bg-amber-50/80"
                        : "border-emerald-200 bg-emerald-50/80"
                }`}
            >
                <div className="flex min-w-0 items-start justify-between gap-2">
                    <div className="min-w-0">
                        <p className="break-words font-semibold text-slate-900">
                            {schedule.staffName}
                        </p>
                        <p className="mt-0.5 break-words text-[11px] text-slate-500">
                            {schedule.role}
                        </p>
                    </div>

                    {schedule.status === "Đã hủy" && (
                        <span className="shrink-0 rounded-full bg-amber-100 px-2 py-1 text-[10px] font-medium text-amber-700">
                            Đã hủy
                        </span>
                    )}
                </div>

                <p className="mt-1 break-words text-[11px] text-slate-600">
                    Phòng: {scheduleDetails.room}
                </p>
            </div>
        );
    };

    const renderDetailStatusBadge = (status: WorkSchedule["status"]) =>
        status === "Đã hủy" ? (
            <span className="inline-flex rounded-full bg-amber-100 px-2 py-1 text-[11px] font-medium text-amber-700">
                Đã hủy
            </span>
        ) : (
            <span className="inline-flex rounded-full bg-emerald-100 px-2 py-1 text-[11px] font-medium text-emerald-700">
                Đã phân công
            </span>
        );

    const renderDetailTableRow = (schedule: WorkSchedule, index: number) => {
        const scheduleDetails = getScheduleDetails(schedule, scheduleOptions);

        return (
            <tr key={schedule.id} className="border-b border-slate-100 last:border-b-0">
                <td className="whitespace-nowrap px-3 py-3 text-xs text-slate-600">{index + 1}</td>
                <td className="px-3 py-3 text-xs font-medium text-slate-900">
                    <div className="max-w-[220px] whitespace-normal break-words">
                        {schedule.staffName}
                    </div>
                </td>
                <td className="px-3 py-3 text-xs text-slate-700">
                    <div className="max-w-[160px] whitespace-normal break-words">
                        {schedule.role}
                    </div>
                </td>
                <td className="px-3 py-3 text-xs text-slate-700">
                    <div className="max-w-[160px] whitespace-normal break-words">
                        {scheduleDetails.room}
                    </div>
                </td>
                <td className="px-3 py-3 text-xs text-slate-700">
                    <div className="max-w-[180px] whitespace-normal break-words">
                        {scheduleDetails.position}
                    </div>
                </td>
                <td className="px-3 py-3 text-xs text-slate-600">
                    <div className="max-w-[220px] whitespace-normal break-words line-clamp-2">
                        {schedule.note || "-"}
                    </div>
                </td>
                <td className="px-3 py-3 text-xs">{renderDetailStatusBadge(schedule.status)}</td>
                <td className="px-3 py-3 text-xs">
                    <div className="flex flex-wrap gap-2">
                        <button
                            type="button"
                            onClick={() => openEdit(schedule)}
                            className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 font-semibold text-slate-700 transition hover:bg-slate-50"
                        >
                            Sửa
                        </button>

                        {schedule.status !== "Đã hủy" && (
                            <button
                                type="button"
                                onClick={() => openCancel(schedule)}
                                className="rounded-xl border border-rose-200 bg-white px-3 py-1.5 font-semibold text-rose-700 transition hover:bg-rose-50"
                            >
                                Hủy
                            </button>
                        )}
                    </div>
                </td>
            </tr>
        );
    };

    return (
        <div className="w-full min-w-0 max-w-full space-y-6 overflow-x-hidden">
            <div className="min-w-0">
                <h1 className="text-2xl font-semibold text-slate-900">
                    Quản lý lịch làm việc nhân sự
                </h1>
                <p className="mt-1 text-sm text-slate-500">
                    Phân công, theo dõi phòng làm việc, vị trí trực và ca làm việc của nhân sự trong
                    trung tâm.
                </p>
            </div>

            <div className="grid w-full min-w-0 grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
                <div className="min-w-0 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                    <p className="text-sm text-slate-500">Tổng lịch làm việc</p>
                    <p className="mt-2 text-2xl font-semibold text-slate-900">{total}</p>
                </div>

                <div className="min-w-0 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                    <p className="text-sm text-slate-500">Đang phân công</p>
                    <p className="mt-2 text-2xl font-semibold text-emerald-700">{assigned}</p>
                </div>

                <div className="min-w-0 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                    <p className="text-sm text-slate-500">Đã hủy</p>
                    <p className="mt-2 text-2xl font-semibold text-amber-700">{cancelled}</p>
                </div>

                <div className="min-w-0 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                    <p className="text-sm text-slate-500">Nhân sự có lịch</p>
                    <p className="mt-2 text-2xl font-semibold text-blue-700">{staffCount}</p>
                </div>
            </div>

            <Card
                title="Tạo lịch tuần"
                subtitle="Copy lịch từ tuần mẫu; nếu chưa có mẫu thì tự xếp theo nhân sự và ca đang hoạt động."
            >
                <div className="grid w-full min-w-0 grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Tuần nguồn
                        </label>
                        <input
                            type="date"
                            value={planSourceWeek}
                            onChange={(event) => {
                                setPlanSourceWeek(event.target.value);
                                setPlanPreview(null);
                            }}
                            className={inputClassName}
                        />
                    </div>
                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Tuần đích
                        </label>
                        <input
                            type="date"
                            value={planTargetWeek}
                            onChange={(event) => {
                                setPlanTargetWeek(event.target.value);
                                setPlanPreview(null);
                            }}
                            className={inputClassName}
                        />
                    </div>
                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Vai trò
                        </label>
                        <select
                            value={planRoleId}
                            onChange={(event) => {
                                setPlanRoleId(event.target.value);
                                setPlanPreview(null);
                            }}
                            className={selectClassName}
                        >
                            <option value="">Tất cả vai trò</option>
                            {scheduleOptions.roles.map((role) => (
                                <option key={role.id} value={role.id}>
                                    {roleFromBackend(role.code)}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Ca
                        </label>
                        <select
                            value={planShiftId}
                            onChange={(event) => {
                                setPlanShiftId(event.target.value);
                                setPlanPreview(null);
                            }}
                            className={selectClassName}
                        >
                            <option value="">Tất cả ca</option>
                            {scheduleOptions.shifts.map((shift) => (
                                <option key={shift.id} value={shift.id}>
                                    {shift.shiftName}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                {planError && <p className="mt-3 text-sm font-medium text-rose-600">{planError}</p>}

                {planPreview && (
                    <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <div className="text-sm text-slate-700">
                                <span className="font-semibold">{planPreview.items.length}</span>{" "}
                                lịch trong preview,
                                <span className="ml-1 font-semibold text-rose-700">
                                    {planPreview.skippedCount}
                                </span>{" "}
                                conflict.
                            </div>
                            <Button
                                onClick={() => void applyPlan()}
                                disabled={
                                    planLoading ||
                                    planPreview.items.length === planPreview.skippedCount
                                }
                            >
                                {planLoading ? "Đang áp dụng..." : "Áp dụng lịch tuần"}
                            </Button>
                        </div>
                        <div className="mt-3 max-h-56 overflow-y-auto rounded-2xl border border-slate-200 bg-white">
                            <table className="min-w-full text-left text-xs">
                                <thead className="bg-slate-50 text-slate-500">
                                    <tr>
                                        <th className="px-3 py-2 font-medium">Nhân sự</th>
                                        <th className="px-3 py-2 font-medium">Ngày đích</th>
                                        <th className="px-3 py-2 font-medium">Ca</th>
                                        <th className="px-3 py-2 font-medium">Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {planPreview.items.slice(0, 30).map((item) => (
                                        <tr
                                            key={`${item.sourceScheduleId ?? item.staffId}-${item.targetDate}-${item.shiftId}`}
                                        >
                                            <td className="px-3 py-2 font-medium text-slate-900">
                                                {item.staffName ?? "-"}
                                            </td>
                                            <td className="px-3 py-2">{item.targetDate}</td>
                                            <td className="px-3 py-2">
                                                {item.shiftName ?? item.shiftCode ?? "-"}
                                            </td>
                                            <td className="px-3 py-2">
                                                <Tag tone={item.conflict ? "red" : "green"}>
                                                    {item.conflict
                                                        ? "Trùng lịch"
                                                        : item.createdScheduleId
                                                          ? "Đã tạo"
                                                          : "Có thể tạo"}
                                                </Tag>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}

                <div className="mt-5 flex flex-wrap items-center gap-3">
                    <Button onClick={() => void previewPlan()} disabled={planLoading}>
                        {planLoading ? "Đang preview..." : "Preview lịch tuần"}
                    </Button>
                    <Button variant="outline" onClick={openCreate} disabled={planLoading}>
                        Thêm thủ công
                    </Button>
                </div>
            </Card>

            <Card
                title="Bộ lọc tìm kiếm"
                subtitle="Tìm theo nhân sự, vai trò, phòng làm việc, vị trí làm việc, ngày làm việc, ca và trạng thái"
            >
                <div className="grid w-full min-w-0 grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Nhân sự
                        </label>
                        <AutocompleteInput
                            value={filters.keyword}
                            onChange={(value) => updateFilter("keyword", value)}
                            options={scheduleOptions.staff.map((s) => ({
                                id: s.fullName,
                                label: s.fullName,
                            }))}
                            placeholder="Chọn hoặc nhập tên nhân sự"
                            allowFreeText={true}
                        />
                    </div>

                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Vai trò
                        </label>
                        <AutocompleteInput
                            value={filters.role}
                            onChange={(value) => updateFilter("role", value)}
                            options={scheduleOptions.roles.map((r) => {
                                const mappedName = roleFromBackend(r.code);
                                return { id: mappedName, label: mappedName };
                            })}
                            placeholder="Tất cả vai trò"
                            allowFreeText={true}
                        />
                    </div>

                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Phòng làm việc
                        </label>
                        <input
                            type="text"
                            value={filters.room}
                            onChange={(event) => updateFilter("room", event.target.value)}
                            onKeyDown={(event) => {
                                if (event.key === "Enter") {
                                    void runSearch();
                                }
                            }}
                            className={inputClassName}
                            placeholder="Nhập tên phòng (VD: P01)"
                        />
                    </div>

                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Ngày làm việc
                        </label>
                        <input
                            type="date"
                            value={filters.workDate}
                            onChange={(event) => updateFilter("workDate", event.target.value)}
                            className={inputClassName}
                        />
                    </div>

                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Ca làm việc
                        </label>
                        <select
                            value={filters.shift}
                            onChange={(event) => updateFilter("shift", event.target.value)}
                            className={selectClassName}
                        >
                            <option value="">Tất cả ca</option>
                            {shiftOptions.map((shift) => (
                                <option key={shift} value={shift}>
                                    {shift}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="min-w-0">
                        <label className="mb-2 block text-sm font-semibold text-slate-700">
                            Trạng thái
                        </label>
                        <select
                            value={filters.status}
                            onChange={(event) => updateFilter("status", event.target.value)}
                            className={selectClassName}
                        >
                            <option value="">Tất cả trạng thái</option>
                            {statusOptions.map((status) => (
                                <option key={status} value={status}>
                                    {status}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                {searchError && (
                    <p className="mt-3 text-sm font-medium text-rose-600">{searchError}</p>
                )}

                <div className="mt-5 flex flex-wrap items-center gap-3">
                    <Button onClick={() => void runSearch()} disabled={loading}>
                        Tìm kiếm
                    </Button>

                    <Button
                        variant="outline"
                        onClick={() => void resetFilters()}
                        disabled={loading}
                    >
                        Xóa bộ lọc / Làm mới
                    </Button>
                </div>
            </Card>

            {feedback && (
                <Card className="border-emerald-200 bg-emerald-50 text-emerald-800">
                    {feedback}
                </Card>
            )}

            <Card
                title="Lịch làm việc theo tuần"
                subtitle="Mỗi ca chỉ hiển thị tóm tắt. Bấm Chi tiết để xem toàn bộ nhân sự trong ca."
            >
                <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                    <div className="flex flex-wrap items-center gap-2">
                        <Button
                            variant="outline"
                            className="px-3 py-2 text-xs"
                            onClick={goToPreviousWeek}
                        >
                            Tuần trước
                        </Button>

                        <Button
                            variant="ghost"
                            className="px-3 py-2 text-xs"
                            onClick={goToCurrentWeek}
                        >
                            Tuần hiện tại
                        </Button>

                        <Button
                            variant="outline"
                            className="px-3 py-2 text-xs"
                            onClick={goToNextWeek}
                        >
                            Tuần sau
                        </Button>
                    </div>

                    <div className="flex flex-wrap items-center gap-3">
                        <label className="flex items-center gap-2 text-xs font-semibold text-slate-600">
                            Chọn tuần
                            <input
                                type="date"
                                value={formatISODate(weekAnchor)}
                                onChange={(event) => goToSelectedWeek(event.target.value)}
                                className="h-9 rounded-2xl border border-slate-200 bg-white px-3 text-xs text-slate-700 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-50"
                            />
                        </label>

                        <div className="text-sm font-medium text-slate-700">
                            {formatWeekRange(weekAnchor)}
                        </div>

                        <Button
                            variant="outline"
                            className="px-4 py-2 text-xs"
                            onClick={openCreate}
                        >
                            Thêm thủ công
                        </Button>
                    </div>
                </div>

                {loading ? (
                    <div className="py-10 text-center text-sm text-slate-500">
                        Đang tải dữ liệu lịch làm việc...
                    </div>
                ) : visibleSchedules.length === 0 ? (
                    <div className="rounded-3xl border border-dashed border-slate-200 bg-slate-50 px-6 py-12 text-center">
                        <p className="text-sm font-semibold text-slate-700">
                            Chưa có lịch làm việc trong tuần này
                        </p>
                        <p className="mt-2 text-sm text-slate-500">
                            Chọn nhân sự, ca và vị trí làm việc từ hệ thống để tạo lịch mới.
                        </p>
                    </div>
                ) : (
                    <div className="grid w-full min-w-0 grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-7">
                        {groupedByDate.map((day) => (
                            <div
                                key={formatISODate(day.date)}
                                className="min-w-0 rounded-3xl border border-slate-200 bg-slate-50/80 p-3"
                            >
                                <div className="mb-3 rounded-2xl bg-white px-3 py-3 shadow-sm">
                                    <p className="text-sm font-semibold text-slate-900">
                                        {formatDayHeading(day.date)}
                                    </p>
                                    <p className="mt-1 text-xs text-slate-500">
                                        {formatFullDate(day.date)}
                                    </p>
                                </div>

                                <div className="space-y-3">
                                    {day.shifts.map((shiftBlock) => {
                                        const previewItems = shiftBlock.items.slice(0, 2);
                                        const hiddenCount =
                                            shiftBlock.items.length - previewItems.length;

                                        return (
                                            <div
                                                key={`${formatISODate(day.date)}-${shiftBlock.shift}`}
                                                className="rounded-3xl bg-white p-3 shadow-sm"
                                            >
                                                <div className="mb-3 flex items-center justify-between gap-2">
                                                    <div>
                                                        <p className="text-xs font-semibold text-slate-900">
                                                            {shiftBlock.shift}
                                                        </p>
                                                        <p className="text-[11px] text-slate-500">
                                                            {shiftBlock.items.length} lịch
                                                        </p>
                                                    </div>

                                                    {shiftBlock.items.length > 0 ? (
                                                        <span className="rounded-full bg-emerald-50 px-2 py-1 text-[10px] font-medium text-emerald-700">
                                                            Có lịch
                                                        </span>
                                                    ) : (
                                                        <span className="rounded-full bg-slate-100 px-2 py-1 text-[10px] font-medium text-slate-500">
                                                            Trống
                                                        </span>
                                                    )}
                                                </div>

                                                {shiftBlock.items.length === 0 ? (
                                                    <div className="w-full rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-3 py-4 text-center text-xs font-semibold text-slate-500">
                                                        Chưa phân công
                                                    </div>
                                                ) : (
                                                    <>
                                                        <div className="space-y-2">
                                                            {previewItems.map((schedule) =>
                                                                renderSchedulePreview(schedule)
                                                            )}
                                                        </div>

                                                        {hiddenCount > 0 && (
                                                            <div className="mt-2 rounded-2xl bg-slate-50 px-3 py-2 text-center text-xs font-semibold text-slate-600">
                                                                +{hiddenCount} lịch khác
                                                            </div>
                                                        )}

                                                        <div className="mt-3 grid grid-cols-1 gap-2">
                                                            <button
                                                                type="button"
                                                                onClick={() =>
                                                                    setDetailSlot({
                                                                        date: day.date,
                                                                        shift: shiftBlock.shift,
                                                                    })
                                                                }
                                                                className="min-w-0 rounded-xl border border-blue-200 bg-blue-50 px-2 py-1.5 text-xs font-semibold text-blue-700 transition hover:bg-blue-100"
                                                            >
                                                                Chi tiết
                                                            </button>
                                                        </div>
                                                    </>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </Card>

            {detailSlot && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4 py-6">
                    <div className="flex max-h-[88vh] w-full max-w-6xl flex-col overflow-hidden rounded-3xl bg-white shadow-2xl">
                        <div className="border-b border-slate-200 px-6 py-5">
                            <div className="flex flex-wrap items-start justify-between gap-3">
                                <div>
                                    <h2 className="text-xl font-semibold text-slate-900">
                                        Chi tiết lịch làm việc
                                    </h2>
                                    <p className="mt-1 text-sm text-slate-500">
                                        {formatDayHeading(detailSlot.date)} - {detailSlot.shift}
                                    </p>
                                    <p className="mt-1 text-xs text-slate-500">
                                        Tổng số lịch: {detailSlotItems.length}
                                    </p>
                                </div>

                                <button
                                    type="button"
                                    onClick={() => setDetailSlot(null)}
                                    className="rounded-2xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                                >
                                    Đóng
                                </button>
                            </div>
                        </div>

                        <div className="border-b border-slate-200 px-6 py-4">
                            <input
                                value={detailSearch}
                                onChange={(event) => setDetailSearch(event.target.value)}
                                className={inputClassName}
                                placeholder="Tìm theo tên, vai trò, phòng, vị trí..."
                            />
                        </div>

                        <div className="flex-1 overflow-hidden px-6 py-5">
                            {detailSlotItems.length === 0 ? (
                                <div className="flex h-full items-center justify-center rounded-3xl border border-dashed border-slate-200 py-10 text-center text-sm text-slate-500">
                                    Chưa có lịch làm việc trong ca này.
                                </div>
                            ) : (
                                <div className="max-h-[55vh] overflow-y-auto rounded-3xl border border-slate-200">
                                    <table className="min-w-full table-fixed border-collapse text-left text-sm">
                                        <thead className="sticky top-0 z-10 bg-slate-50">
                                            <tr className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                                                <th className="w-14 px-3 py-3">STT</th>
                                                <th className="px-3 py-3">Nhân sự</th>
                                                <th className="px-3 py-3">Vai trò</th>
                                                <th className="px-3 py-3">Phòng</th>
                                                <th className="px-3 py-3">Vị trí</th>
                                                <th className="px-3 py-3">Ghi chú</th>
                                                <th className="w-32 px-3 py-3">Trạng thái</th>
                                                <th className="w-40 px-3 py-3">Thao tác</th>
                                            </tr>
                                        </thead>

                                        <tbody className="bg-white">
                                            {detailSlotItems.map((schedule, index) =>
                                                renderDetailTableRow(schedule, index)
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>

                        <div className="flex flex-wrap justify-end gap-3 border-t border-slate-200 px-6 py-4">
                            <Button
                                variant="outline"
                                onClick={() => openCreateForSlot(detailSlot.date, detailSlot.shift)}
                            >
                                Thêm thủ công
                            </Button>

                            <Button variant="outline" onClick={() => setDetailSlot(null)}>
                                Đóng
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            <WorkScheduleFormDialog
                open={formOpen}
                mode={formMode}
                initialValue={formValue}
                options={scheduleOptions}
                loading={formLoading}
                error={formError}
                optionError={optionError}
                onClose={() => setFormOpen(false)}
                onSubmit={submitForm}
                currentSchedule={editingSchedule}
            />

            <WorkScheduleCancelDialog
                open={Boolean(cancelTarget)}
                schedule={cancelTarget}
                loading={cancelLoading}
                error={cancelError}
                onClose={() => setCancelTarget(null)}
                onConfirm={() => void confirmCancel()}
            />
        </div>
    );
}
