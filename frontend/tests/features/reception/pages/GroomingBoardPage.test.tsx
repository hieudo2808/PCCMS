import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { groomingApi } from "~/features/grooming/api/groomingApi";
import { GroomingBoardPage } from "~/features/reception/pages/GroomingBoardPage";

vi.mock("~/features/grooming/api/groomingApi", () => ({
    groomingApi: {
        getTickets: vi.fn(),
        getStations: vi.fn(),
        confirmTicket: vi.fn(),
        startTicket: vi.fn(),
        completeTicket: vi.fn(),
        cancelTicket: vi.fn(),
    },
}));

vi.mock("react-hot-toast", () => ({
    default: {
        success: vi.fn(),
        error: vi.fn(),
    },
}));

function renderWithQueryClient(ui: React.ReactElement) {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("GroomingBoardPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(groomingApi.getStations).mockResolvedValue([
            { id: "station-1", stationCode: "SPA-01", name: "Bàn spa 1", isActive: true },
        ]);
        vi.mocked(groomingApi.getTickets).mockResolvedValue({
            content: [
                {
                    id: "ticket-1",
                    appointmentId: "appointment-1",
                    serviceOrderId: "order-1",
                    orderCode: "SO-001",
                    serviceOrderStatus: "REQUESTED",
                    ownerId: "owner-1",
                    ownerName: "Owner",
                    petId: "pet-1",
                    petName: "Milu",
                    serviceId: "service-1",
                    serviceCode: "GRM-BATH",
                    serviceName: "Tam say",
                    basePriceVnd: 100000,
                    durationMinutes: 60,
                    scheduledStartAt: new Date().toISOString(),
                    scheduledEndAt: new Date(new Date().getTime() + 3600000).toISOString(),
                    appointmentStatus: "PENDING",
                    statusCode: "PENDING",
                    ownerNote: "Can nhe tay",
                    estimatedAmountVnd: 100000,
                },
            ],
            pageNumber: 1,
            pageSize: 20,
            totalElements: 1,
            totalPages: 1,
            isLast: true,
        });
        vi.mocked(groomingApi.confirmTicket).mockResolvedValue({} as never);
    });

    it("should_confirm_pending_ticket_with_station", async () => {
        renderWithQueryClient(<GroomingBoardPage />);

        await screen.findByText("Milu");
        await userEvent.selectOptions(screen.getByRole("combobox"), "station-1");
        await userEvent.click(screen.getByRole("button", { name: /Xác nhận/i }));

        await waitFor(() => {
            expect(groomingApi.confirmTicket).toHaveBeenCalledWith("ticket-1", "station-1");
        });
    });
});
