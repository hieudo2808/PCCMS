import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BoardingLogPage } from "~/features/reception/pages/BoardingLogPage";
import { boardingApi, roomAdminApi } from "~/features/boarding/api/boardingApi";
import type { BoardingBookingResponse, CareLogResponse, RoomResponse } from "~/types/boarding";

vi.mock("~/features/boarding/api/boardingApi", () => ({
    boardingApi: {
        getBookings: vi.fn(),
        getCareLogs: vi.fn(),
        createCareLog: vi.fn(),
        confirmBooking: vi.fn(),
        checkIn: vi.fn(),
        startStay: vi.fn(),
        checkOut: vi.fn(),
    },
    roomAdminApi: {
        getRooms: vi.fn(),
    },
}));
vi.mock("react-hot-toast", () => ({
    default: { success: vi.fn(), error: vi.fn() },
}));
vi.mock("~/shared/auth/tokenStorage", () => ({
    hasAccessToken: () => true,
}));
vi.mock("~/features/auth/context/AuthContext", () => ({
    useAuth: () => ({
        isAuthenticated: true,
        user: { id: "staff-1", roleCode: "STAFF", fullName: "Staff" },
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
];

const savedLog: CareLogResponse = {
    id: "log-1",
    sessionId: "session-1",
    logDate: "2026-06-05",
    periodCode: "MORNING",
    feedingStatus: "An tot",
    hygieneStatus: "Sach",
    staffNote: "Ghi chu",
    staffId: "staff-1",
    staffName: "Staff",
    createdAt: "2026-06-05T10:00:00",
    media: [],
};

function renderPage() {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } },
    });
    return render(
        <QueryClientProvider client={queryClient}>
            <BoardingLogPage />
        </QueryClientProvider>
    );
}

describe("BoardingLogPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(boardingApi.getBookings).mockResolvedValue(pageResponse(mockBookings));
        vi.mocked(roomAdminApi.getRooms).mockResolvedValue(pageResponse<RoomResponse>([]));
        vi.mocked(boardingApi.getCareLogs).mockResolvedValue([]);
        vi.mocked(boardingApi.createCareLog).mockResolvedValue(savedLog);
    });

    it("renders active boarding bookings", async () => {
        renderPage();

        await waitFor(() => {
            expect(screen.getAllByText("Milu").length).toBeGreaterThan(0);
            expect(screen.getByText("Owner One")).toBeInTheDocument();
            expect(screen.getByText("BKG-001")).toBeInTheDocument();
        });
    });

    it("saves care log for selected booking", async () => {
        const user = userEvent.setup();
        const { container } = renderPage();

        const saveButton = await screen.findByRole("button", { name: /u nh/i });
        fireEvent.change(container.querySelector('input[type="date"]')!, {
            target: { value: "2026-06-05" },
        });
        const textboxes = screen.getAllByRole("textbox");
        await user.type(textboxes[0], "An tot");
        await user.type(textboxes[1], "Sach");

        await user.click(saveButton);

        await waitFor(() => {
            expect(boardingApi.createCareLog).toHaveBeenCalledWith(
                expect.objectContaining({
                    sessionId: "session-1",
                    logDate: "2026-06-05",
                    periodCode: "MORNING",
                    feedingStatus: "An tot",
                    hygieneStatus: "Sach",
                })
            );
        });
    });
});
