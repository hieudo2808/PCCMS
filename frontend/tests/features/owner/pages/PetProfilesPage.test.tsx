import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PetProfilesPage } from "~/features/owner/pages/PetProfilesPage";
import { petApi } from "~/shared/api/petApi";

vi.mock("~/shared/api/petApi");

const queryClient = new QueryClient({
    defaultOptions: {
        queries: { retry: false },
    },
});

const renderComponent = () => {
    return render(
        <QueryClientProvider client={queryClient}>
            <PetProfilesPage />
        </QueryClientProvider>
    );
};

const mockPet = {
    id: "pet-1",
    ownerId: "owner-1",
    name: "Milu",
    speciesId: "CHÓ",
    breedId: "Poodle",
    sex: "MALE" as const,
    birthDate: "2023-01-01",
    estimatedAgeMonths: 24,
    weightKg: 5,
    color: "Brown",
    identificationNote: "",
    specialNote: "",
    allergyNote: "",
    nutritionNote: "",
    isActive: true,
    healthAlerts: [
        {
            id: "alert-1",
            petId: "pet-1",
            medicalRecordId: "mr-1",
            severity: "WARNING" as const,
            message: "Chưa tiêm phòng dại",
            createdAt: "2024-01-01",
        },
    ],
};

describe("PetProfilesPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        queryClient.clear();
    });

    it("renders loading state initially", () => {
        vi.mocked(petApi.getPets).mockReturnValue(new Promise(() => {}));
        renderComponent();
        expect(screen.getByText("Hồ sơ thú cưng")).toBeInTheDocument();
    });

    it("renders empty state when no pets", async () => {
        vi.mocked(petApi.getPets).mockResolvedValueOnce({
            content: [],
            pageNumber: 1,
            pageSize: 20,
            totalElements: 0,
            totalPages: 1,
            isLast: true,
        });

        renderComponent();

        await waitFor(() => {
            expect(screen.getByText("Chưa có thú cưng nào")).toBeInTheDocument();
        });
    });

    it("renders list of pets with health alerts", async () => {
        vi.mocked(petApi.getPets).mockResolvedValueOnce({
            content: [mockPet],
            pageNumber: 1,
            pageSize: 20,
            totalElements: 1,
            totalPages: 1,
            isLast: true,
        });

        renderComponent();

        await waitFor(() => {
            expect(screen.getByText("Milu")).toBeInTheDocument();
        });
        expect(screen.getByText("CHÓ • Poodle")).toBeInTheDocument();
        expect(screen.getByText("5 kg")).toBeInTheDocument();
        expect(screen.getByText("Cảnh báo: Chưa tiêm phòng dại")).toBeInTheDocument();
    });

    it("opens and validates the add pet modal", async () => {
        vi.mocked(petApi.getPets).mockResolvedValueOnce({
            content: [],
            pageNumber: 1,
            pageSize: 20,
            totalElements: 0,
            totalPages: 1,
            isLast: true,
        });

        renderComponent();

        const addButton = await screen.findByRole("button", { name: /Thêm thú cưng/i });
        await userEvent.click(addButton);

        // Modal should appear
        expect(screen.getByText("Thêm thú cưng mới")).toBeInTheDocument();

        // Submit empty form to trigger validation
        const submitButton = screen.getByRole("button", { name: "Lưu thú cưng" });
        await userEvent.click(submitButton);

        await waitFor(() => {
            expect(screen.getByText("Vui lòng nhập tên thú cưng")).toBeInTheDocument();
        });
    });

    it("creates a new pet successfully", async () => {
        vi.mocked(petApi.getPets).mockResolvedValueOnce({
            content: [],
            pageNumber: 1,
            pageSize: 20,
            totalElements: 0,
            totalPages: 1,
            isLast: true,
        });

        // Mock create pet response
        vi.mocked(petApi.createPet).mockResolvedValueOnce(mockPet);

        renderComponent();

        const addButton = await screen.findByRole("button", { name: /Thêm thú cưng/i });
        await userEvent.click(addButton);

        const nameInput = screen.getByPlaceholderText("Milu");
        await userEvent.type(nameInput, "Milu");

        // Mặc định Loài là mảng [CHÓ, MÈO, THỎ, CHIM], the first option is CHÓ, so it's already selected.

        const submitButton = screen.getByRole("button", { name: "Lưu thú cưng" });
        await userEvent.click(submitButton);

        await waitFor(() => {
            expect(petApi.createPet).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: "Milu",
                })
            );
        });
    });
});
