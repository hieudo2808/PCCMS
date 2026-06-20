import type { NotificationResponse } from "~/shared/api/notificationApi";
import type { RoleKey } from "~/types/navigation";

export function notificationCenterPath(role: RoleKey) {
    return role === "public" ? "/login" : `/${role}/notifications`;
}

export function notificationTarget(notification: NotificationResponse, role: RoleKey) {
    if (role === "owner") {
        switch (notification.sourceType) {
            case "APPOINTMENT":
                return "/owner/appointments";
            case "GROOMING":
            case "GROOMING_TICKET":
                return "/owner/grooming/tracking";
            case "BOARDING":
                return "/owner/boarding/tracking";
            case "INVOICE":
            case "PAYMENT":
                return "/owner/payments";
        }
    }
    if (notification.sourceType === "ACCOUNT") {
        return `/${role}/profile`;
    }
    return notificationCenterPath(role);
}
