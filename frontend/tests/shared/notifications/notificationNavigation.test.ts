import { describe, expect, it } from "vitest";
import type { NotificationResponse } from "~/shared/api/notificationApi";
import { notificationCenterPath, notificationTarget } from "~/shared/notifications/notificationNavigation";

const notification = (sourceType: string): NotificationResponse => ({
    id: "n-1",
    recipientUserId: "u-1",
    sourceType,
    sourceId: "s-1",
    notificationType: "TEST",
    title: "Thông báo",
    body: "Nội dung",
    statusCode: "UNREAD",
    readAt: null,
    createdAt: "2026-06-20T08:00:00+07:00",
    updatedAt: "2026-06-20T08:00:00+07:00",
});

describe("notification navigation", () => {
    it("maps owner business sources to their detail areas", () => {
        expect(notificationTarget(notification("APPOINTMENT"), "owner")).toBe("/owner/appointments");
        expect(notificationTarget(notification("GROOMING"), "owner")).toBe("/owner/grooming/tracking");
        expect(notificationTarget(notification("BOARDING"), "owner")).toBe("/owner/boarding/tracking");
        expect(notificationTarget(notification("INVOICE"), "owner")).toBe("/owner/payments");
    });

    it("keeps unknown sources in the current role notification center", () => {
        expect(notificationTarget(notification("UNKNOWN"), "admin")).toBe("/admin/notifications");
        expect(notificationCenterPath("veterinarian")).toBe("/veterinarian/notifications");
    });
});
