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
            "unifiedBooking",
            "boardingTracking",
            "payments",
            "profile",
        ],
    },
    reception: {
        label: "Nhân viên",
        screens: ["receptionDashboard", "appointmentReception", "groomingBoard", "boardingLog"],
    },
    doctor: {
        label: "Bác sĩ",
        screens: ["doctorDashboard", "doctorQueue", "medicalRecord"],
    },
    admin: {
        label: "Quản trị viên",
        screens: ["adminDashboard", "accounts", "catalog", "rooms", "schedule", "reports"],
    },
};
