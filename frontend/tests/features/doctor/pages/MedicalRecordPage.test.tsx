import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MedicalRecordPage } from "~/features/doctor/pages/MedicalRecordPage";
import { medicalRecordApi } from "~/shared/api/medicalRecordApi";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import userEvent from "@testing-library/user-event";
import type { RecordStatus } from "~/types/medicalRecord";

// Mock the API
vi.mock("~/shared/api/medicalRecordApi", () => ({
    medicalRecordApi: {
        getMedicalRecordById: vi.fn(),
        updateMedicalRecord: vi.fn(),
        finalizeMedicalRecord: vi.fn(),
        createPrescription: vi.fn(),
        listPrescriptions: vi.fn(),
    },
}));

vi.mock("~/shared/api/petApi", () => ({
    petApi: {
        getPetById: vi.fn().mockResolvedValue({
            id: "pet-123",
            name: "Milu",
            speciesName: "Chó",
            breedName: "Poodle",
            sex: "MALE",
        }),
    },
}));

// Mock toast
vi.mock("react-hot-toast", () => ({
    toast: {
        success: vi.fn(),
        error: vi.fn(),
    },
}));

const mockRecord = {
    id: "record-123",
    recordCode: "MR-001",
    petId: "pet-123",
    veterinarianId: "vet-123",
    recordStatus: "DRAFT" as RecordStatus,
    temperatureC: 39,
    heartRateBpm: 120,
    weightKg: 5,
} as any;

const renderWithProviders = (ui: React.ReactElement, initialEntries = ["/records/record-123"]) => {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={initialEntries}>
                <Routes>
                    <Route path="/records/:id" element={ui} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe("MedicalRecordPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(medicalRecordApi.listPrescriptions).mockResolvedValue([]);
    });

    it("renders loading state initially", () => {
        vi.mocked(medicalRecordApi.getMedicalRecordById).mockReturnValue(new Promise(() => {})); // Never resolves
        const { container } = renderWithProviders(<MedicalRecordPage />);
        expect(container.querySelector(".animate-spin")).toBeInTheDocument();
    });

    it("loads and displays medical record data", async () => {
        vi.mocked(medicalRecordApi.getMedicalRecordById).mockResolvedValue(mockRecord);
        renderWithProviders(<MedicalRecordPage />);

        await waitFor(() => {
            expect(screen.getByText("Chi tiết bệnh án: MR-001")).toBeInTheDocument();
        });

        // Check if initial values are set in the form
        await waitFor(() => {
            expect(screen.getByDisplayValue("39")).toBeInTheDocument(); // Temperature
            expect(screen.getByDisplayValue("120")).toBeInTheDocument(); // Heart Rate
        });
    });

    it("disables all inputs and buttons when record is FINALIZED", async () => {
        vi.mocked(medicalRecordApi.getMedicalRecordById).mockResolvedValue({
            ...mockRecord,
            recordStatus: "FINALIZED",
        } as any);

        renderWithProviders(<MedicalRecordPage />);

        await waitFor(() => {
            expect(screen.getByText("Đã chốt")).toBeInTheDocument();
        });

        // Inputs should be disabled
        const inputs = screen.getAllByRole("spinbutton");
        inputs.forEach((input) => {
            expect(input).toBeDisabled();
        });

        // Save and Finalize buttons should not be present
        expect(screen.queryByRole("button", { name: /Lưu nháp/i })).not.toBeInTheDocument();
        expect(screen.queryByRole("button", { name: /Chốt bệnh án/i })).not.toBeInTheDocument();
    });

    it("calls finalize API when Chốt bệnh án is clicked", async () => {
        vi.mocked(medicalRecordApi.getMedicalRecordById).mockResolvedValue(mockRecord);
        vi.mocked(medicalRecordApi.finalizeMedicalRecord).mockResolvedValue({
            ...mockRecord,
            recordStatus: "FINALIZED",
        } as any);

        renderWithProviders(<MedicalRecordPage />);

        await waitFor(() => {
            expect(screen.getByText("Chi tiết bệnh án: MR-001")).toBeInTheDocument();
        });

        // Enter required fields for finalization
        const diagInput = screen.getByLabelText(/Chẩn đoán xác định/i);
        await userEvent.type(diagInput, "Bệnh X");

        const finalizeBtn = screen.getByRole("button", { name: /Chốt bệnh án/i });
        await userEvent.click(finalizeBtn);

        await waitFor(() => {
            expect(medicalRecordApi.finalizeMedicalRecord).toHaveBeenCalledWith(
                "record-123",
                expect.objectContaining({
                    finalDiagnosis: "Bệnh X",
                })
            );
        });
    });
});
