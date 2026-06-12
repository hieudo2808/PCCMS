import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ShiftChangeRequestPage } from "~/features/admin/work-schedule-management/pages/ShiftChangeRequestPage";
import { getAdminShiftChangeRequests } from "~/features/admin/work-schedule-management/adminShiftChangeRequestService";
import { BrowserRouter } from "react-router-dom";

vi.mock("~/features/admin/work-schedule-management/adminShiftChangeRequestService", () => ({
    getAdminShiftChangeRequests: vi.fn(),
    respondToAdminShiftChangeRequest: vi.fn(),
}));

describe("ShiftChangeRequestPage", () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({
            defaultOptions: { queries: { retry: false } },
        });
        vi.clearAllMocks();
    });

    const renderPage = () => {
        return render(
            <QueryClientProvider client={queryClient}>
                <BrowserRouter>
                    <ShiftChangeRequestPage />
                </BrowserRouter>
            </QueryClientProvider>
        );
    };

    it("renders loading state initially", () => {
        (getAdminShiftChangeRequests as any).mockResolvedValue({ content: [], totalElements: 0, totalPages: 0 });
        renderPage();
        expect(screen.getByTestId("loading-spinner")).toBeInTheDocument();
    });

    it("renders table with requests after loading", async () => {
        (getAdminShiftChangeRequests as any).mockResolvedValue({
            content: [
                {
                    id: "req-1",
                    requestedByStaffName: "Nguyen Van A",
                    targetStaffName: "Le Thi B",
                    reason: "Ban viec gia dinh",
                    statusCode: "PENDING",
                    workDate: "2026-06-12",
                    shiftName: "Ca sang",
                },
            ],
            totalElements: 1,
            totalPages: 1,
        });

        renderPage();

        await waitFor(() => {
            expect(screen.getByText("Nguyen Van A")).toBeInTheDocument();
            expect(screen.getByText("Le Thi B")).toBeInTheDocument();
            expect(screen.getByText("Ban viec gia dinh")).toBeInTheDocument();
        });
    });

    it("opens the detail modal when clicking on the details button", async () => {
        (getAdminShiftChangeRequests as any).mockResolvedValue({
            content: [
                {
                    id: "req-1",
                    requestedByStaffName: "Nguyen Van A",
                    targetStaffName: "Le Thi B",
                    reason: "Ban viec gia dinh",
                    statusCode: "PENDING",
                    workDate: "2026-06-12",
                    shiftName: "Ca sang",
                },
            ],
            totalElements: 1,
            totalPages: 1,
        });

        renderPage();

        await waitFor(() => {
            expect(screen.getByText("Nguyen Van A")).toBeInTheDocument();
        });

        const detailsButton = screen.getByRole("button", { name: /Xem chi tiết/i });
        detailsButton.click();

        expect(await screen.findByRole("dialog")).toBeInTheDocument();
        expect(screen.getByText("Chi tiết yêu cầu đổi ca")).toBeInTheDocument();
        // Since Modal renders children, we verify content inside
        expect(screen.getAllByText(/Nguyen Van A/).length).toBeGreaterThan(0);
        expect(screen.getAllByText(/Le Thi B/).length).toBeGreaterThan(0);
    });
});
