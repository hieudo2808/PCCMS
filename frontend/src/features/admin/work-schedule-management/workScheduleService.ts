import api, { getApiData, getPageContent } from "~/api/api";
import type {
    BackendScheduleStatus,
    WorkSchedule,
    WorkScheduleExamRoomOption,
    WorkScheduleFormValues,
    WorkScheduleGroomingStationOption,
    WorkScheduleOptions,
    WorkScheduleRole,
    WorkScheduleRoleOption,
    WorkScheduleSearchParams,
    WorkScheduleShift,
    WorkScheduleShiftOption,
    WorkScheduleStaffOption,
    WorkScheduleStatus,
    WeeklySchedulePlanRequest,
    WeeklySchedulePlanResponse,
} from "./types";

interface BackendWorkSchedule {
    id: string;
    staffId?: string;
    staffName?: string;
    workDate?: string;
    shiftId?: string;
    shiftCode?: string;
    shiftName?: string;
    startTime?: string;
    endTime?: string;
    examRoomId?: string;
    examRoomCode?: string;
    examRoomName?: string;
    stationId?: string;
    stationCode?: string;
    stationName?: string;
    roleId?: string;
    roleCode?: string;
    capacity?: number;
    statusCode?: BackendScheduleStatus;
    note?: string;
}

interface WorkScheduleDateRange {
    fromDate: string;
    toDate: string;
}

interface WorkSchedulePayload {
    staffId: string;
    workDate: string;
    shiftId: string;
    examRoomId: string | null;
    stationId: string | null;
    roleId: string;
    capacity: number;
    statusCode: BackendScheduleStatus;
    note: string;
}

const DEFAULT_CAPACITY = 1;
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

const toIsoDate = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
};

const addDays = (date: Date, days: number) => {
    const clone = new Date(date);
    clone.setDate(clone.getDate() + days);
    return clone;
};

const getStartOfWeek = (date: Date) => {
    const clone = new Date(date);
    clone.setHours(0, 0, 0, 0);
    clone.setDate(clone.getDate() - clone.getDay());
    return clone;
};

const getCurrentWeekRange = (): WorkScheduleDateRange => {
    const start = getStartOfWeek(new Date());
    return {
        fromDate: toIsoDate(start),
        toDate: toIsoDate(addDays(start, 6)),
    };
};

const normalizeDate = (value?: string) => {
    if (!value) return "";

    const trimmed = value.trim();
    if (!trimmed) return "";

    if (trimmed.includes("/")) {
        const [day, month, year] = trimmed.split("/");
        if (!day || !month || !year) return trimmed;
        return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
    }

    return trimmed;
};

let schedulesStore: WorkSchedule[] = [];

const cloneSchedule = (schedule: WorkSchedule): WorkSchedule => ({ ...schedule });

const statusFromBackend = (status?: BackendScheduleStatus): WorkScheduleStatus => {
    switch (status) {
        case "CANCELLED":
            return "Đã hủy";
        case "COMPLETED":
            return "Đã hoàn thành";
        default:
            return "Đã phân công";
    }
};

const statusToBackend = (status: WorkScheduleStatus | ""): BackendScheduleStatus => {
    switch (status) {
        case "Đã hủy":
            return "CANCELLED";
        case "Đã hoàn thành":
            return "COMPLETED";
        default:
            return "ASSIGNED";
    }
};

const shiftFromBackend = (item: BackendWorkSchedule): WorkScheduleShift => {
    const value = `${item.shiftCode ?? ""} ${item.shiftName ?? ""}`.toLowerCase();
    if (value.includes("evening") || value.includes("tối") || value.includes("toi")) {
        return "Ca tối";
    }
    if (value.includes("afternoon") || value.includes("chiều") || value.includes("chieu")) {
        return "Ca chiều";
    }
    return "Ca sáng";
};

export const roleFromBackend = (roleCode?: string): WorkScheduleRole => {
    switch (roleCode) {
        case "VETERINARIAN":
            return "Bác sĩ thú y";
        default:
            return "Nhân viên trung tâm";
    }
};

const toSchedule = (item: BackendWorkSchedule, index: number): WorkSchedule => ({
    id: item.id,
    scheduleCode: `WS${String(index + 1).padStart(3, "0")}`,
    staffId: item.staffId ?? "",
    staffName: item.staffName ?? "",
    shiftId: item.shiftId,
    roleId: item.roleId,
    examRoomId: item.examRoomId,
    stationId: item.stationId,
    capacity: item.capacity ?? DEFAULT_CAPACITY,
    statusCode: item.statusCode,
    role: roleFromBackend(item.roleCode),
    room: item.examRoomId ? "Khu khám bệnh" : item.stationId ? "Khu spa" : "",
    position: item.examRoomId
        ? [item.examRoomCode, item.examRoomName].filter(Boolean).join(" - ")
        : item.stationId
            ? [item.stationCode, item.stationName].filter(Boolean).join(" - ")
            : "",
    workDate: item.workDate ?? "",
    shift: shiftFromBackend(item),
    status: statusFromBackend(item.statusCode),
    note: item.note ?? "",
    source: "backend",
});

const validateRequired = (payload: WorkScheduleFormValues) => {
    if (!payload.staffId || !payload.shiftId || !payload.roleId || !payload.workDate || !payload.status) {
        throw new Error("Chưa có đủ nhân sự, ca, vai trò, ngày làm việc và trạng thái từ hệ thống");
    }
};

const toPayload = (payload: WorkScheduleFormValues): WorkSchedulePayload => {
    validateRequired(payload);
    if (!payload.examRoomId && !payload.stationId) {
        throw new Error("Vui lòng chọn phòng hoặc vị trí làm việc trước khi lưu lịch");
    }

    return {
        staffId: payload.staffId,
        workDate: normalizeDate(payload.workDate),
        shiftId: payload.shiftId,
        examRoomId: payload.examRoomId || null,
        stationId: payload.stationId || null,
        roleId: payload.roleId,
        capacity: DEFAULT_CAPACITY,
        statusCode: statusToBackend(payload.status),
        note: payload.note.trim(),
    };
};

const getDateRange = (params?: WorkScheduleSearchParams, fallbackRange: WorkScheduleDateRange = getCurrentWeekRange()) => {
    if (params?.workDate) {
        const workDate = normalizeDate(params.workDate);
        return { fromDate: workDate, toDate: workDate };
    }
    return fallbackRange;
};

const filterLocally = (items: WorkSchedule[], params: WorkScheduleSearchParams) => {
    const keyword = params.keyword.trim().toLowerCase();

    return items.filter((schedule) => {
        const matchesKeyword = !keyword || schedule.staffName.toLowerCase().includes(keyword);
        const matchesRole = !params.role || schedule.role === params.role;
        const matchesRoom = !params.room || schedule.position.toLowerCase().includes(params.room.trim().toLowerCase());
        const matchesDate = !params.workDate || normalizeDate(schedule.workDate) === normalizeDate(params.workDate);
        const matchesShift = !params.shift || schedule.shift === params.shift;
        const matchesStatus = !params.status || schedule.status === params.status;
        return matchesKeyword && matchesRole && matchesRoom && matchesDate && matchesShift && matchesStatus;
    });
};

export const getWorkScheduleOptions = async (): Promise<WorkScheduleOptions> => {
    const [staff, shifts, roles, examRooms, groomingStations] = await Promise.all([
        api.get("/v1/admin/work-schedules/options/staff"),
        api.get("/v1/admin/work-schedules/options/shifts"),
        api.get("/v1/admin/work-schedules/options/roles"),
        api.get("/v1/admin/work-schedules/options/exam-rooms"),
        api.get("/v1/admin/work-schedules/options/grooming-stations"),
    ]);

    return {
        staff: getApiData<WorkScheduleStaffOption[]>(staff),
        shifts: getApiData<WorkScheduleShiftOption[]>(shifts),
        roles: getApiData<WorkScheduleRoleOption[]>(roles),
        examRooms: getApiData<WorkScheduleExamRoomOption[]>(examRooms),
        groomingStations: getApiData<WorkScheduleGroomingStationOption[]>(groomingStations),
    };
};

export const getWorkSchedules = async (range: WorkScheduleDateRange = getCurrentWeekRange()) => {
    const response = await api.get("/v1/admin/work-schedules", {
        params: { ...range, page: 0, size: 100 },
    });
    const schedules = getPageContent<BackendWorkSchedule>(getApiData<unknown>(response)).map(toSchedule);
    schedulesStore = schedules;
    return schedulesStore.map(cloneSchedule);
};

export const searchWorkSchedules = async (params: WorkScheduleSearchParams) => {
    const range = getDateRange(params);
    const response = await api.get("/v1/admin/work-schedules", {
        params: { ...range, page: 0, size: 100 },
    });
    const schedules = getPageContent<BackendWorkSchedule>(getApiData<unknown>(response)).map(toSchedule);
    schedulesStore = schedules;
    return filterLocally(schedules, params).map(cloneSchedule);
};

export const createWorkSchedule = async (payload: WorkScheduleFormValues) => {
    const response = await api.post("/v1/admin/work-schedules", toPayload(payload));
    const created = toSchedule(getApiData<BackendWorkSchedule>(response), 0);
    schedulesStore = [created, ...schedulesStore];
    return cloneSchedule(created);
};

export const updateWorkSchedule = async (id: string, payload: WorkScheduleFormValues) => {
    if (!uuidPattern.test(id)) {
        throw new Error("Chỉ có thể cập nhật lịch đã lưu trên hệ thống");
    }

    const response = await api.put(`/v1/admin/work-schedules/${id}`, toPayload(payload));
    const updated = toSchedule(getApiData<BackendWorkSchedule>(response), 0);
    schedulesStore = schedulesStore.map((schedule) => (schedule.id === id ? updated : schedule));
    return cloneSchedule(updated);
};

export const cancelWorkSchedule = async (id: string) => {
    if (!uuidPattern.test(id)) {
        throw new Error("Chỉ có thể hủy lịch đã lưu trên hệ thống");
    }

    const response = await api.delete(`/v1/admin/work-schedules/${id}`);
    const updated = toSchedule(getApiData<BackendWorkSchedule>(response), 0);
    schedulesStore = schedulesStore.map((schedule) => (schedule.id === id ? updated : schedule));
    return cloneSchedule(updated);
};

export const previewWeeklySchedulePlan = async (payload: WeeklySchedulePlanRequest) => {
    const response = await api.post("/v1/admin/work-schedules/weekly-plan/preview", payload);
    return getApiData<WeeklySchedulePlanResponse>(response);
};

export const applyWeeklySchedulePlan = async (payload: WeeklySchedulePlanRequest) => {
    const response = await api.post("/v1/admin/work-schedules/weekly-plan/apply", payload);
    return getApiData<WeeklySchedulePlanResponse>(response);
};
