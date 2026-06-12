import { describe, it, expect, vi, beforeEach } from "vitest";
import { searchAccounts } from "./account-management/accountService";
import { getRooms } from "./room-management/roomService";
import { getMedicines } from "./medicine-management/medicineService";
import { getServices } from "./service-category-management/serviceCategoryService";
import { getWorkSchedules } from "./work-schedule-management/workScheduleService";
import { getDashboardSummary } from "./dashboard/dashboardService";
import { getReportData } from "./report-management/reportService";
import api from "~/shared/api/axiosClient";

vi.mock("~/shared/api/axiosClient", () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        patch: vi.fn(),
        delete: vi.fn(),
    },
}));

describe("Admin Services API Endpoints", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should call /v1/admin/accounts in accountService", async () => {
        (api.get as any).mockResolvedValueOnce({
            content: [],
            pageNumber: 1,
            pageSize: 10,
            totalElements: 0,
            totalPages: 1,
            isLast: true,
        });
        await searchAccounts({ fullName: "admin" });
        expect(api.get).toHaveBeenCalledWith("/v1/admin/accounts", expect.any(Object));
    });

    it("should call /v1/admin/rooms in roomService", async () => {
        (api.get as any).mockResolvedValueOnce({
            content: [],
            pageNumber: 1,
            pageSize: 10,
            totalElements: 0,
            totalPages: 1,
            isLast: true,
        });
        await getRooms();
        expect(api.get).toHaveBeenCalledWith("/v1/admin/rooms", expect.any(Object));
    });

    it("should call /v1/medicines in medicineService", async () => {
        (api.get as any).mockResolvedValueOnce({
            content: [],
            pageNumber: 1,
            pageSize: 10,
            totalElements: 0,
            totalPages: 1,
            isLast: true,
        });
        await getMedicines();
        expect(api.get).toHaveBeenCalledWith("/v1/medicines", expect.any(Object));
    });

    it("should call /v1/catalog/services in serviceCategoryService", async () => {
        (api.get as any).mockResolvedValueOnce({
            content: [],
            pageNumber: 1,
            pageSize: 10,
            totalElements: 0,
            totalPages: 1,
            isLast: true,
        });
        await getServices();
        expect(api.get).toHaveBeenCalledWith("/v1/catalog/services", expect.any(Object));
    });

    it("should call /v1/admin/work-schedules in workScheduleService", async () => {
        (api.get as any).mockResolvedValueOnce({
            content: [],
            pageNumber: 1,
            pageSize: 10,
            totalElements: 0,
            totalPages: 1,
            isLast: true,
        });
        await getWorkSchedules({ fromDate: "2024-01-01", toDate: "2024-01-07" });
        expect(api.get).toHaveBeenCalledWith("/v1/admin/work-schedules", expect.any(Object));
    });

    it("should call /v1/admin/reports in reportService", async () => {
        (api.get as any).mockResolvedValueOnce({
            records: [],
            summary: {},
        });
        await getReportData({ reportType: "REVENUE", group: "ALL", fromDate: "2026-01-01", toDate: "2026-06-07", serviceId: "" });
        expect(api.get).toHaveBeenCalledWith("/v1/admin/reports/summary", expect.any(Object));
    });

    it("should call multiple /v1/admin APIs in dashboardService", async () => {
        (api.get as any).mockResolvedValue({
            content: [],
            records: [],
            summary: {},
        });
        await getDashboardSummary();
        expect(api.get).toHaveBeenCalledWith("/v1/admin/accounts", expect.any(Object));
        expect(api.get).toHaveBeenCalledWith("/v1/catalog/services", expect.any(Object));
        expect(api.get).toHaveBeenCalledWith("/v1/admin/rooms", expect.any(Object));
        expect(api.get).toHaveBeenCalledWith("/v1/admin/reports/summary", expect.any(Object));
    });
});
