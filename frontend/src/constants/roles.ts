import type { RoleConfig, RoleKey } from "~/types/navigation";

/** Role definitions with their accessible screens */
export const roles: Record<RoleKey, RoleConfig> = {
    public: {
        label: "Khách",
        screens: ["login", "register", "forgot"],
    },
    owner: {
        label: "Chủ nuôi",
        screens: [
            "ownerDashboard",
            "pets",
            "ownerAppointments",
            "unifiedBooking",
            "groomingBooking",
            "groomingTracking",
            "boardingTracking",
            "payments",
            "profile",
        ],
    },
    staff: {
        label: "Nhân viên",
        screens: ["receptionDashboard", "appointmentReception", "groomingBoard", "boardingLog", "receptionMySchedule", "staffProfile"],
    },
    veterinarian: {
        label: "Bác sĩ",
        screens: ["doctorDashboard", "doctorQueue", "medicalRecord", "doctorMySchedule", "vetProfile"],
    },
    admin: {
        label: "Quản trị viên",
        screens: ["adminDashboard", "accounts", "catalog", "rooms", "schedule", "adminShiftRequests", "reports", "adminProfile"],
    },
};
