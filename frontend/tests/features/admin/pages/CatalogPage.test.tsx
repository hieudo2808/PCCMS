import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { CatalogPage } from "~/features/admin/pages/CatalogPage";
import * as medicineService from "~/features/admin/medicine-management/medicineService";
import * as serviceCategoryService from "~/features/admin/service-category-management/serviceCategoryService";
import { catalogApi } from "~/features/admin/api/catalogApi";
import type { Medicine } from "~/features/admin/medicine-management/types";
import type { Service } from "~/features/admin/service-category-management/types";

// ─── Mocks ─────────────────────────────────────────────────────────────────

vi.mock("~/features/admin/medicine-management/medicineService", () => ({
    getMedicines: vi.fn(),
    createMedicine: vi.fn(),
    updateMedicine: vi.fn(),
    deleteMedicine: vi.fn(),
}));

vi.mock("~/features/admin/service-category-management/serviceCategoryService", () => ({
    getServices: vi.fn(),
    createService: vi.fn(),
    updateService: vi.fn(),
    deleteService: vi.fn(),
}));

vi.mock("~/features/admin/api/catalogApi", () => ({
    catalogApi: {
        listMedicineCategories: vi.fn(),
        createMedicineCategory: vi.fn(),
        updateMedicineCategory: vi.fn(),
    },
}));

vi.mock("react-hot-toast", () => ({
    toast: { success: vi.fn(), error: vi.fn() },
}));

// ─── Test Data ──────────────────────────────────────────────────────────────

const mockMedicines: Medicine[] = [
    {
        id: "med-1",
        code: "MED-001",
        name: "Amoxicillin",
        categoryId: "cat-1",
        group: "Kháng sinh",
        unit: "Hộp",
        stock: 124,
        unitPriceVnd: 100000,
        note: "",
    },
];

const mockServices: Service[] = [
    {
        id: "svc-1",
        code: "SVC-001",
        name: "Khám tổng quát",
        type: "Khám bệnh",
        price: 200000,
        durationMinutes: 30,
        description: "Khám sức khỏe định kỳ",
        status: "active",
    },
];

const mockMedicineCategories = [
    { id: "cat-1", name: "Kháng sinh", description: "Nhóm thuốc kháng sinh", isActive: true } as any,
];

// ─── Helper ─────────────────────────────────────────────────────────────────

const renderWithProviders = (ui: React.ReactElement) => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

// ─── Tests ──────────────────────────────────────────────────────────────────

describe("CatalogPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(medicineService.getMedicines).mockResolvedValue(mockMedicines);
        vi.mocked(serviceCategoryService.getServices).mockResolvedValue(mockServices);
        vi.mocked(catalogApi.listMedicineCategories).mockResolvedValue(mockMedicineCategories as any);
    });

    it("renders loading state initially", () => {
        vi.mocked(medicineService.getMedicines).mockReturnValue(new Promise(() => {}));
        const { container } = renderWithProviders(<CatalogPage />);
        expect(container.querySelector(".animate-spin")).toBeInTheDocument();
    });

    it("displays list of medicines in Thuốc tab", async () => {
        renderWithProviders(<CatalogPage />);

        await waitFor(() => {
            expect(screen.getByText("Amoxicillin")).toBeInTheDocument();
        });
        expect(screen.getByText("124")).toBeInTheDocument();
        expect(screen.getByText("Kháng sinh")).toBeInTheDocument();
    });

    it("switches to Dịch vụ tab and shows services", async () => {
        renderWithProviders(<CatalogPage />);

        const svcTab = screen.getByRole("button", { name: /Dịch vụ/i });
        await userEvent.click(svcTab);

        await waitFor(() => {
            expect(screen.getByText("Khám tổng quát")).toBeInTheDocument();
        });
        expect(screen.getByText("SVC-001")).toBeInTheDocument();
    });

    it("opens create medicine modal when clicking Thêm thuốc mới", async () => {
        renderWithProviders(<CatalogPage />);

        await waitFor(() => {
            expect(screen.getByText("Amoxicillin")).toBeInTheDocument();
        });

        const addBtn = screen.getByRole("button", { name: /Thêm thuốc mới/i });
        await userEvent.click(addBtn);

        expect(screen.getByRole("dialog")).toBeInTheDocument();
        expect(screen.getByText("Thêm Thuốc Mới")).toBeInTheDocument();
    });

    it("opens create service modal when clicking Thêm dịch vụ mới", async () => {
        renderWithProviders(<CatalogPage />);

        const svcTab = screen.getByRole("button", { name: /Dịch vụ/i });
        await userEvent.click(svcTab);

        await waitFor(() => {
            expect(screen.getByText("Khám tổng quát")).toBeInTheDocument();
        });

        const addBtn = screen.getByRole("button", { name: /Thêm dịch vụ mới/i });
        await userEvent.click(addBtn);

        expect(screen.getByRole("dialog")).toBeInTheDocument();
        expect(screen.getByText("Thêm Dịch Vụ Mới")).toBeInTheDocument();
    });

    it("submits create medicine form successfully", async () => {
        vi.mocked(medicineService.createMedicine).mockResolvedValue(mockMedicines[0]);
        renderWithProviders(<CatalogPage />);

        await waitFor(() => {
            expect(screen.getByText("Amoxicillin")).toBeInTheDocument();
        });

        // Open modal
        await userEvent.click(screen.getByRole("button", { name: /Thêm thuốc mới/i }));
        expect(screen.getByRole("dialog")).toBeInTheDocument();

        await userEvent.type(screen.getByLabelText(/Tên thuốc/i), "Vitamin C");
        await userEvent.selectOptions(screen.getByLabelText(/Nhóm thuốc/i), "cat-1");
        await userEvent.type(screen.getByLabelText(/Đơn vị/i), "Vỉ");
        await userEvent.type(screen.getByLabelText(/Tồn kho ban đầu/i), "100");
        await userEvent.type(screen.getByLabelText(/Giá \(VND\)/i), "50000");

        // Submit
        const submitBtn = screen.getByRole("button", { name: /^Lưu$/i });
        await userEvent.click(submitBtn);

        await waitFor(() => {
            expect(medicineService.createMedicine).toHaveBeenCalled();
        });
    });

    it("opens Nhóm thuốc tab", async () => {
        vi.mocked(catalogApi.listMedicineCategories).mockResolvedValue([
            { id: "cat-1", name: "Kháng sinh", description: "Nhóm thuốc kháng sinh", isActive: true } as any,
        ] as any);

        renderWithProviders(<CatalogPage />);

        const catTab = screen.getByRole("button", { name: /Nhóm thuốc/i });
        await userEvent.click(catTab);

        await waitFor(() => {
            expect(screen.getByText("Kháng sinh")).toBeInTheDocument();
        });
    });
});
