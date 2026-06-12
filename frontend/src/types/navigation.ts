import type { ComponentType, SVGProps } from "react";

export type RoleKey = "public" | "owner" | "staff" | "veterinarian" | "admin";

export type ScreenKey =
    // Auth
    | "login"
    | "register"
    | "forgot"
    // Owner
    | "ownerDashboard"
    | "pets"
    | "ownerAppointments"
    | "unifiedBooking"
    | "groomingBooking"
    | "groomingTracking"
    | "boardingTracking"
    | "payments"
    | "profile"
    // Reception
    | "receptionDashboard"
    | "appointmentReception"
    | "groomingBoard"
    | "boardingLog"
    | "receptionMySchedule"
    // Doctor
    | "doctorDashboard"
    | "doctorQueue"
    | "medicalRecord"
    | "doctorMySchedule"
    // Admin
    | "adminDashboard"
    | "accounts"
    | "catalog"
    | "rooms"
    | "schedule"
    | "adminShiftRequests"
    | "reports"
    | "staffProfile"
    | "vetProfile"
    | "adminProfile";


export type IconComponent = ComponentType<SVGProps<SVGSVGElement> & { className?: string }>;

export interface RoleConfig {
    label: string;
    screens: ScreenKey[];
}

export interface ScreenMetaItem {
    label: string;
    icon: IconComponent;
    path: string;
}
