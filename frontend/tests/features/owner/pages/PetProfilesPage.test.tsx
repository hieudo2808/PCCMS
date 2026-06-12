import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PetProfilesPage } from "~/features/owner/pages/PetProfilesPage";
import { petApi } from "~/shared/api/petApi";

vi.mock("~/shared/api/petApi");
vi.mock("~/shared/api/petCatalogApi", () => ({
    petCatalogApi: {
        getSpecies: vi.fn().mockResolvedValue([{ id: "species-1", name: "CHÓ" }]),
        getBreedsBySpecies: vi.fn().mockResolvedValue([{ id: "breed-1", name: "Poodle" }]),
    }
}));

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
    speciesId: "species-1",
    speciesName: "CHÓ",
    breedId: "breed-1",
    breedName: "Poodle",
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

    afterEach(() => {
        vi.restoreAllMocks();
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

        // Wait for species options to load
        await screen.findByRole("option", { name: "CHÓ" });
        const speciesSelect = screen.getByLabelText(/Loài/i);
        await userEvent.selectOptions(speciesSelect, "species-1");

        const submitButton = screen.getByRole("button", { name: "Lưu thú cưng" });
        await userEvent.click(submitButton);

        await waitFor(() => {
            expect(petApi.createPet).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: "Milu",
                    speciesId: "species-1",
                })
            );
        });
    });

    it("opens edit modal and updates a pet", async () => {
        vi.mocked(petApi.getPets).mockResolvedValueOnce({
            content: [mockPet],
            pageNumber: 1,
            pageSize: 20,
            totalElements: 1,
            totalPages: 1,
            isLast: true,
        });
        vi.mocked(petApi.updatePet).mockResolvedValueOnce({ ...mockPet, weightKg: 6 });

        renderComponent();

        await screen.findByText("Milu");
        await userEvent.click(screen.getByRole("button", { name: "Chỉnh sửa" }));

        expect(screen.getByText("Chỉnh sửa hồ sơ thú cưng")).toBeInTheDocument();

        const weightInput = screen.getByLabelText(/Cân nặng/i);
        await userEvent.clear(weightInput);
        await userEvent.type(weightInput, "6");
        await userEvent.click(screen.getByRole("button", { name: "Cập nhật thú cưng" }));

        await waitFor(() => {
            expect(petApi.updatePet).toHaveBeenCalledWith(
                "pet-1",
                expect.objectContaining({
                    name: "Milu",
                    speciesId: "species-1",
                    weightKg: 6,
                })
            );
        });
    });

    it("confirms and hides a pet profile", async () => {
        vi.spyOn(window, "confirm").mockReturnValue(true);
        vi.mocked(petApi.getPets).mockResolvedValueOnce({
            content: [mockPet],
            pageNumber: 1,
            pageSize: 20,
            totalElements: 1,
            totalPages: 1,
            isLast: true,
        });
        vi.mocked(petApi.deletePet).mockResolvedValueOnce();

        renderComponent();

        await screen.findByText("Milu");
        await userEvent.click(screen.getByRole("button", { name: "Ẩn hồ sơ Milu" }));

        await waitFor(() => {
            expect(petApi.deletePet).toHaveBeenCalledWith("pet-1");
        });
    });
});
