import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "~/api/api";
import { createMedicine } from "~/features/admin/medicine-management/medicineService";

vi.mock("~/api/api", () => ({
    default: {
        post: vi.fn(),
    },
    getApiData: vi.fn((response) => response.data),
    getPageContent: vi.fn(),
}));

describe("medicine management service", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("accepts seeded medicine category UUIDs from the backend", async () => {
        const seededCategoryId = "aaaaaaaa-aaaa-aaaa-aaaa-000000000001";
        vi.mocked(api.post).mockResolvedValue({
            data: {
                id: "bbbbbbbb-bbbb-bbbb-bbbb-000000000001",
                medicineCode: "MED-001",
                name: "Amoxicillin",
                categoryId: seededCategoryId,
                categoryName: "Kháng sinh",
                unit: "viên",
                currentStock: 10,
                unitPriceVnd: 5000,
                isActive: true,
            },
        });

        await createMedicine({
            code: "",
            name: "Amoxicillin",
            categoryId: seededCategoryId,
            group: "Kháng sinh",
            unit: "viên",
            stock: "10",
            unitPriceVnd: "5000",
            note: "",
        });

        expect(api.post).toHaveBeenCalledWith("/v1/medicines", {
            medicineCode: undefined,
            name: "Amoxicillin",
            categoryId: seededCategoryId,
            unit: "viên",
            currentStock: 10,
            unitPriceVnd: 5000,
        });
    });
});
