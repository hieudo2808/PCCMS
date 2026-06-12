import api, { getApiData, getPageContent } from "~/api/api";

type BackendScheduleStatus = "ASSIGNED" | "CANCELLED" | "COMPLETED";
type BackendRequestStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED";

interface BackendWorkSchedule {
    id: string;
    staffId?: string;
    staffName?: string;
    workDate?: string;
    shiftCode?: string;
    shiftName?: string;
    startTime?: string;
    endTime?: string;
    roleCode?: string;
    statusCode?: BackendScheduleStatus;
    note?: string;
}

interface BackendShiftChangeRequest {
    id: string;
    scheduleId: string;
    requestedBy?: string;
    targetStaff?: string;
    reason?: string;
    statusCode?: BackendRequestStatus;
    createdAt?: string;
}

export interface PersonalScheduleItem {
    id: string;
    userId: string;
    userName: string;
    role: string;
    workDate: string;
    shift: string;
    startTime: string;
    endTime: string;
    status: string;
    note: string;
}

export interface ShiftChangeRequestItem {
    requestId: string;
    scheduleId: string;
    senderName: string;
    receiverName: string;
    reason: string;
    status: string;
    createdAt: string;
    scheduleDisplay?: string;
}

export interface ShiftTargetStaffOption {
    id: string;
    fullName: string;
    roleCode?: string;
    roleName?: string;
}

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export const isBackendId = (value: string) => uuidPattern.test(value);

const shiftFromBackend = (item: BackendWorkSchedule) => {
    const value = `${item.shiftCode ?? ""} ${item.shiftName ?? ""}`.toLowerCase();
    if (value.includes("evening") || value.includes("tối") || value.includes("toi")) return "Ca tối";
    if (value.includes("afternoon") || value.includes("chiều") || value.includes("chieu")) return "Ca chiều";
    return "Ca sáng";
};

const scheduleStatusFromBackend = (status?: BackendScheduleStatus) => {
    switch (status) {
        case "CANCELLED":
            return "Đã hủy";
        case "COMPLETED":
            return "Đã hoàn thành";
        default:
            return "Đã phân công";
    }
};

const requestStatusFromBackend = (status?: BackendRequestStatus) => {
    switch (status) {
        case "ACCEPTED":
            return "Đã đồng ý";
        case "REJECTED":
            return "Từ chối";
        case "CANCELLED":
            return "Đã hủy";
        default:
            return "Đang chờ";
    }
};

const roleFromBackend = (roleCode?: string) => {
    switch (roleCode) {
        case "VETERINARIAN":
            return "Bác sĩ thú y";
        case "ADMIN":
            return "Quản trị viên";
        case "STAFF":
            return "Nhân viên trung tâm";
        default:
            return "Lễ tân";
    }
};

const toPersonalSchedule = (item: BackendWorkSchedule): PersonalScheduleItem => ({
    id: item.id,
    userId: item.staffId ?? "",
    userName: item.staffName ?? "Tôi",
    role: roleFromBackend(item.roleCode),
    workDate: item.workDate ?? "",
    shift: shiftFromBackend(item),
    startTime: item.startTime ?? "",
    endTime: item.endTime ?? "",
    status: scheduleStatusFromBackend(item.statusCode),
    note: item.note ?? "",
});

const toShiftChangeRequest = (item: BackendShiftChangeRequest): ShiftChangeRequestItem => ({
    requestId: item.id,
    scheduleId: item.scheduleId,
    senderName: "Tôi",
    receiverName: item.targetStaff ?? "",
    reason: item.reason ?? "",
    status: requestStatusFromBackend(item.statusCode),
    createdAt: item.createdAt ?? "",
});

const toIncomingShiftChangeRequest = (item: BackendShiftChangeRequest): ShiftChangeRequestItem => ({
    requestId: item.id,
    scheduleId: item.scheduleId,
    senderName: item.requestedBy ?? "Ẩn danh",
    receiverName: "Tôi",
    reason: item.reason ?? "",
    status: requestStatusFromBackend(item.statusCode),
    createdAt: item.createdAt ?? "",
});

export const getMyWorkSchedules = async (fromDate: string, toDate: string) => {
    const response = await api.get("/v1/me/work-schedules", {
        params: { fromDate, toDate, page: 0, size: 100 },
    });
    return getPageContent<BackendWorkSchedule>(getApiData<unknown>(response)).map(toPersonalSchedule);
};

export const getMyShiftChangeRequests = async () => {
    const response = await api.get("/v1/me/shift-change-requests", {
        params: { page: 0, size: 100 },
    });
    return getPageContent<BackendShiftChangeRequest>(getApiData<unknown>(response)).map(toShiftChangeRequest);
};

export const getShiftTargetStaffOptions = async () => {
    const response = await api.get("/v1/work-schedules/options/staff");
    return getApiData<ShiftTargetStaffOption[]>(response);
};

export const createMyShiftChangeRequest = async (scheduleId: string, reason: string, targetStaffId?: string) => {
    const response = await api.post("/v1/me/shift-change-requests", {
        scheduleId,
        reason,
        targetStaffId: targetStaffId || undefined,
    });
    return toShiftChangeRequest(getApiData<BackendShiftChangeRequest>(response));
};

export const cancelMyShiftChangeRequest = async (requestId: string) => {
    const response = await api.patch(`/v1/me/shift-change-requests/${requestId}/cancel`);
    return toShiftChangeRequest(getApiData<BackendShiftChangeRequest>(response));
};

export const getIncomingShiftChangeRequests = async () => {
    const response = await api.get("/v1/shift-change-requests/incoming", {
        params: { page: 0, size: 100 },
    });
    return getPageContent<BackendShiftChangeRequest>(getApiData<unknown>(response)).map(toIncomingShiftChangeRequest);
};

export const respondToIncomingShiftChangeRequest = async (requestId: string, isAccepted: boolean, reason?: string) => {
    const response = await api.patch(`/v1/shift-change-requests/${requestId}/respond`, {
        action: isAccepted ? "ACCEPTED" : "REJECTED",
    });
    return toIncomingShiftChangeRequest(getApiData<BackendShiftChangeRequest>(response));
};
