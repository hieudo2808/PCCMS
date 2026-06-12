import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BoardingTrackingPage } from "~/features/owner/pages/BoardingTrackingPage";
import { boardingApi } from "~/features/boarding/api/boardingApi";
import type { BoardingBookingResponse, CareLogResponse } from "~/types/boarding";

vi.mock("~/features/boarding/api/boardingApi", () => ({
    boardingApi: {
        getMyBookings: vi.fn(),
        getCareLogs: vi.fn(),
    },
}));
vi.mock("~/shared/auth/tokenStorage", () => ({
    hasAccessToken: () => true,
}));
vi.mock("~/features/auth/context/AuthContext", () => ({
    useAuth: () => ({
        isAuthenticated: true,
        user: { id: "1", roleCode: "OWNER", fullName: "Test" },
    }),
}));

function pageResponse<T>(content: T[]) {
    return {
        content,
        pageNumber: 0,
        pageSize: 10,
        totalElements: content.length,
        totalPages: content.length > 0 ? 1 : 0,
        isLast: true,
    };
}

const mockBookings: BoardingBookingResponse[] = [
    {
        id: "booking-1",
        bookingCode: "BKG-001",
        sessionId: "session-1",
        serviceOrderId: "order-1",
        orderCode: "SO-001",
        serviceOrderStatus: "CONFIRMED",
        ownerId: "owner-1",
        ownerName: "Owner One",
        petId: "pet-1",
        petName: "Milu",
        requestedRoomTypeId: "room-type-1",
        requestedRoomTypeName: "STANDARD",
        roomId: "room-1",
        roomCode: "R101",
        roomName: "Room 101",
        expectedCheckinAt: "2026-06-05T09:00:00",
        expectedCheckoutAt: "2026-06-10T09:00:00",
        estimatedPriceVnd: 500000,
        statusCode: "IN_STAY",
    },
    {
        id: "booking-2",
        bookingCode: "BKG-002",
        sessionId: "session-2",
        serviceOrderId: "order-2",
        orderCode: "SO-002",
        serviceOrderStatus: "CONFIRMED",
        ownerId: "owner-1",
        ownerName: "Owner One",
        petId: "pet-2",
        petName: "Bong",
        requestedRoomTypeId: "room-type-1",
        requestedRoomTypeName: "STANDARD",
        roomId: "room-2",
        roomCode: "R102",
        roomName: "Room 102",
        expectedCheckinAt: "2026-06-06T09:00:00",
        expectedCheckoutAt: "2026-06-11T09:00:00",
        estimatedPriceVnd: 500000,
        statusCode: "IN_STAY",
    },
];

const mockLogs: CareLogResponse[] = [
    {
        id: "log-1",
        sessionId: "session-1",
        logDate: "2026-06-05",
        periodCode: "MORNING",
        feedingStatus: "An tot",
        hygieneStatus: "Sach",
        staffNote: "Staff note one",
        staffId: "staff-1",
        staffName: "Staff One",
        createdAt: "2026-06-05T10:00:00",
        media: [],
    },
    {
        id: "log-2",
        sessionId: "session-2",
        logDate: "2026-06-06",
        periodCode: "AFTERNOON",
        feedingStatus: "An vua du",
        hygieneStatus: "On",
        staffNote: "Staff note two",
        staffId: "staff-2",
        staffName: "Staff Two",
        createdAt: "2026-06-06T10:00:00",
        media: [],
    },
];

function renderPage() {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } },
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <BoardingTrackingPage />
        </QueryClientProvider>
    );
}

describe("BoardingTrackingPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("shows empty state when no boarding bookings exist", async () => {
        vi.mocked(boardingApi.getMyBookings).mockResolvedValue(pageResponse([]));

        renderPage();

        await waitFor(() => {
            expect(screen.getAllByText(/booking/i).length).toBeGreaterThan(0);
            expect(boardingApi.getCareLogs).not.toHaveBeenCalled();
        });
    });

    it("renders booking filters and care logs", async () => {
        vi.mocked(boardingApi.getMyBookings).mockResolvedValue(pageResponse(mockBookings));
        vi.mocked(boardingApi.getCareLogs).mockResolvedValue([mockLogs[0]]);

        renderPage();

        await waitFor(() => {
            expect(screen.getAllByText("Milu").length).toBeGreaterThan(0);
            expect(screen.getByText("Bong")).toBeInTheDocument();
        });

        await waitFor(() => {
            expect(screen.getByText(/An tot/)).toBeInTheDocument();
            expect(screen.getByText(/Staff note one/)).toBeInTheDocument();
        });
    });

    it("loads care logs by selected booking", async () => {
        const user = userEvent.setup();
        vi.mocked(boardingApi.getMyBookings).mockResolvedValue(pageResponse(mockBookings));
        vi.mocked(boardingApi.getCareLogs).mockImplementation(async (bookingId: string) => {
            if (bookingId === "booking-2") {
                return [mockLogs[1]];
            }
            return [mockLogs[0]];
        });

        renderPage();

        await waitFor(() => {
            expect(screen.getByText("Bong")).toBeInTheDocument();
        });

        await user.click(screen.getByRole("button", { name: /Bong/ }));

        await waitFor(() => {
            expect(boardingApi.getCareLogs).toHaveBeenCalledWith("booking-2");
        });
    });

    it("shows care log empty state when selected booking has no logs", async () => {
        vi.mocked(boardingApi.getMyBookings).mockResolvedValue(pageResponse(mockBookings));
        vi.mocked(boardingApi.getCareLogs).mockResolvedValue([]);

        renderPage();

        await waitFor(() => {
            expect(
                screen.getByText((content, element) => {
                    return (
                        element?.tagName.toLowerCase() === "h3" &&
                        content.toLowerCase().startsWith("ch") &&
                        content.toLowerCase().includes("nh") &&
                        content.toLowerCase().includes("k")
                    );
                })
            ).toBeInTheDocument();
        });
    });
});
