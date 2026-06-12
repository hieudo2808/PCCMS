import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { groomingApi } from "~/features/grooming/api/groomingApi";
import { GroomingBookingPage } from "~/features/owner/pages/GroomingBookingPage";
import { petApi } from "~/shared/api/petApi";

vi.mock("~/shared/api/petApi", () => ({
    petApi: {
        getPets: vi.fn(),
    },
}));

vi.mock("~/features/grooming/api/groomingApi", () => ({
    groomingApi: {
        getServices: vi.fn(),
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

function futureDateInputValue(daysFromNow = 1) {
    const date = new Date();
    date.setDate(date.getDate() + daysFromNow);
    return date.toISOString().slice(0, 10);
}

describe("GroomingBookingPage", () => {
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
        vi.mocked(groomingApi.getServices).mockResolvedValue([
            {
                id: "service-1",
                serviceCode: "GRM-BATH",
                name: "Tắm sấy",
                basePriceVnd: 100000,
                durationMinutes: 60,
                isActive: true,
            },
        ]);
        vi.mocked(groomingApi.createBooking).mockResolvedValue({} as never);
    });

    it("should_show_estimate_and_submit_grooming_booking", async () => {
        const futureDate = futureDateInputValue();

        renderWithQueryClient(<GroomingBookingPage />);

        await screen.findByText("Milu");
        await userEvent.selectOptions(screen.getByLabelText("Thú cưng"), "pet-1");
        await userEvent.selectOptions(screen.getByLabelText("Dịch vụ làm đẹp"), "service-1");
        fireEvent.change(screen.getByLabelText("Ngày hẹn"), { target: { value: futureDate } });
        fireEvent.change(screen.getByLabelText("Giờ hẹn"), { target: { value: "09:00" } });

        expect(screen.getAllByText(/100.000/).length).toBeGreaterThan(0);
        await userEvent.click(screen.getByRole("button", { name: /Gửi yêu cầu làm đẹp/i }));

        await waitFor(() => {
            expect(groomingApi.createBooking).toHaveBeenCalledWith(
                expect.objectContaining({
                    petId: "pet-1",
                    serviceId: "service-1",
                })
            );
        });
    });

    it("should_keep_selected_date_when_time_changes", async () => {
        const futureDate = futureDateInputValue();

        renderWithQueryClient(<GroomingBookingPage />);

        await screen.findByText("Milu");
        const dateInput = screen.getByLabelText("Ngày hẹn") as HTMLInputElement;
        const timeInput = screen.getByLabelText("Giờ hẹn") as HTMLInputElement;

        fireEvent.change(dateInput, { target: { value: futureDate } });
        fireEvent.change(timeInput, { target: { value: "09:00" } });
        fireEvent.change(timeInput, { target: { value: "14:30" } });

        expect(dateInput.value).toBe(futureDate);
        expect(timeInput.value).toBe("14:30");
    });
});
