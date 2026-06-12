import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { UnifiedBookingPage } from "~/features/owner/pages/UnifiedBookingPage";
import { boardingApi } from "~/features/boarding/api/boardingApi";
import { petApi } from "~/shared/api/petApi";

vi.mock("~/shared/api/petApi", () => ({
    petApi: {
        getPets: vi.fn(),
    },
}));

vi.mock("~/features/boarding/api/boardingApi", () => ({
    boardingApi: {
        getAvailability: vi.fn(),
        createBooking: vi.fn(),
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
    return render(
        <MemoryRouter>
            <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
        </MemoryRouter>
    );
}

describe("UnifiedBookingPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(petApi.getPets).mockResolvedValue({
            content: [
                {
                    id: "pet-1",
                    name: "Milu",
                    ownerId: "owner-1",
                    speciesId: "dog",
                    breedId: "poodle",
                    sex: "MALE",
                    birthDate: "",
                    estimatedAgeMonths: 12,
                    weightKg: 4,
                    color: "",
                    identificationNote: "",
                    specialNote: "",
                    allergyNote: "",
                    nutritionNote: "",
                    isActive: true,
                    healthAlerts: [],
                },
            ],
            pageNumber: 1,
            pageSize: 10,
            totalElements: 1,
            totalPages: 1,
            isLast: true,
        });
        vi.mocked(boardingApi.getAvailability).mockResolvedValue([
            {
                roomTypeId: "type-1",
                roomTypeCode: "VIP",
                roomTypeName: "VIP",
                defaultCapacity: 1,
                baseDailyPriceVnd: 300000,
                availableRooms: 2,
            },
        ]);
        vi.mocked(boardingApi.createBooking).mockResolvedValue({} as never);
    });

    it("should_submit_booking_when_form_is_valid", async () => {
        renderWithQueryClient(<UnifiedBookingPage />);

        await screen.findByText("Milu");
        await userEvent.selectOptions(screen.getByLabelText("Thú cưng"), "pet-1");
        fireEvent.change(screen.getByLabelText("Ngày nhận phòng"), {
            target: { value: "2026-06-20" },
        });
        fireEvent.change(screen.getByLabelText("Giờ nhận phòng"), { target: { value: "09:00" } });
        fireEvent.change(screen.getByLabelText("Ngày trả phòng"), {
            target: { value: "2026-06-22" },
        });
        fireEvent.change(screen.getByLabelText("Giờ trả phòng"), { target: { value: "10:00" } });

        await waitFor(() => expect(boardingApi.getAvailability).toHaveBeenCalled());
        await screen.findByText(/VIP/);
        await userEvent.selectOptions(screen.getByLabelText("Loại phòng"), "type-1");
        fireEvent.change(screen.getByLabelText("Yêu cầu chăm sóc đặc biệt"), {
            target: { value: "Cho ăn hạt riêng" },
        });
        await userEvent.click(screen.getByRole("button", { name: /Gửi yêu cầu đặt phòng/i }));

        await waitFor(() => {
            expect(boardingApi.createBooking).toHaveBeenCalledWith(
                expect.objectContaining({
                    petId: "pet-1",
                    roomTypeId: "type-1",
                    specialCareRequest: "Cho ăn hạt riêng",
                })
            );
        });
    });

    it("should_keep_selected_dates_when_times_change", async () => {
        renderWithQueryClient(<UnifiedBookingPage />);

        await screen.findByText("Milu");
        const checkinDate = screen.getByLabelText("Ngày nhận phòng") as HTMLInputElement;
        const checkinTime = screen.getByLabelText("Giờ nhận phòng") as HTMLInputElement;
        const checkoutDate = screen.getByLabelText("Ngày trả phòng") as HTMLInputElement;
        const checkoutTime = screen.getByLabelText("Giờ trả phòng") as HTMLInputElement;

        fireEvent.change(checkinDate, { target: { value: "2026-06-20" } });
        fireEvent.change(checkinTime, { target: { value: "09:00" } });
        fireEvent.change(checkoutDate, { target: { value: "2026-06-22" } });
        fireEvent.change(checkoutTime, { target: { value: "10:00" } });
        fireEvent.change(checkinTime, { target: { value: "14:30" } });
        fireEvent.change(checkoutTime, { target: { value: "16:00" } });

        expect(checkinDate.value).toBe("2026-06-20");
        expect(checkinTime.value).toBe("14:30");
        expect(checkoutDate.value).toBe("2026-06-22");
        expect(checkoutTime.value).toBe("16:00");
    });
});
