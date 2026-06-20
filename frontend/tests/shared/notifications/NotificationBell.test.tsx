import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { NotificationBell } from "~/shared/notifications/NotificationBell";
import { server } from "@tests/mocks/server";

const baseUrl = "http://localhost:8080/api";

function renderBell() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter><NotificationBell role="owner" /></MemoryRouter>
        </QueryClientProvider>,
    );
}

describe("NotificationBell", () => {
    it("shows unread count and recent notifications", async () => {
        server.use(
            http.get(`${baseUrl}/v1/notifications/unread-count`, () =>
                HttpResponse.json({ success: true, data: { unreadCount: 3 } })),
            http.get(`${baseUrl}/v1/notifications/my`, () =>
                HttpResponse.json({
                    success: true,
                    data: {
                        content: [{
                            id: "n-1", recipientUserId: "u-1", sourceType: "APPOINTMENT", sourceId: "a-1",
                            notificationType: "APPOINTMENT_CONFIRMED", title: "Lịch hẹn đã xác nhận",
                            body: "Lịch hẹn của Milu đã được xác nhận.", statusCode: "UNREAD", readAt: null,
                            createdAt: "2026-06-20T08:00:00+07:00", updatedAt: "2026-06-20T08:00:00+07:00",
                        }],
                        pageNumber: 1, pageSize: 5, totalElements: 1, totalPages: 1, isFirst: true, isLast: true,
                    },
                })),
        );
        const user = userEvent.setup();
        renderBell();

        const trigger = await screen.findByRole("button", { name: "Thông báo, 3 chưa đọc" });
        await user.click(trigger);

        expect(await screen.findByText("Lịch hẹn đã xác nhận")).toBeInTheDocument();
        expect(screen.getByText("3 thông báo chưa đọc")).toBeInTheDocument();
    });
});
