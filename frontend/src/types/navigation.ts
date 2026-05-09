import type { ComponentType, SVGProps } from "react";

export type RoleKey = "public" | "owner" | "reception" | "doctor" | "admin";

export type ScreenKey =
    // Auth
    | "login"
    | "register"
    | "forgot"
    // Owner
    | "ownerDashboard"
    | "pets"
    | "unifiedBooking"
    | "boardingBooking"
    | "boardingTracking"
    | "payments"
    | "profile"
    // Reception
    | "receptionDashboard"
    | "appointmentReception"
    | "groomingBoard"
    | "boardingReception"
    | "boardingLog"
    | "cashier"
    // Doctor
    | "doctorDashboard"
    | "doctorQueue"
    | "medicalRecord"
    // Admin
    | "adminDashboard"
    | "accounts"
    | "catalog"
    | "rooms"
    | "schedule"
    | "reports";

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
