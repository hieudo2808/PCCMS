import { endOfCurrentMonth, formatCompactVnd, formatIsoDate, startOfCurrentMonth } from "../report-management/reportService";
import api, { getApiData } from "~/api/api";

interface DashboardSummary {
    activeAccounts: number;
    activeServices: number;
    availableRooms: number;
    monthlyRevenueVnd: number;
    monthlyInvoiceCount: number;
}

interface PageLike {
    content?: unknown[];
    items?: unknown[];
    totalElements?: number;
    data?: {
        content?: unknown[];
        items?: unknown[];
        totalElements?: number;
    };
}

interface ReportSummaryLike {
    totalRevenueVnd?: number | string;
    totalInvoiceCount?: number;
}

function getTotalCount(payload: unknown): number {
    if (!payload || typeof payload !== "object") {
        return 0;
    }

    const page = payload as PageLike;
    if (typeof page.totalElements === "number") {
        return page.totalElements;
    }
    if (typeof page.data?.totalElements === "number") {
        return page.data.totalElements;
    }
    if (Array.isArray(page.content)) {
        return page.content.length;
    }
    if (Array.isArray(page.items)) {
        return page.items.length;
    }
    if (Array.isArray(page.data?.content)) {
        return page.data.content.length;
    }
    if (Array.isArray(page.data?.items)) {
        return page.data.items.length;
    }
    return 0;
}

async function getPagedCount(url: string, params: Record<string, string | number | boolean>) {
    const response = await api.get(url, { params });
    return getTotalCount(getApiData<unknown>(response));
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
    const fromDate = formatIsoDate(startOfCurrentMonth());
    const toDate = formatIsoDate(endOfCurrentMonth());

    const [activeAccounts, activeServices, availableRooms, reportResponse] = await Promise.all([
        getPagedCount("/v1/admin/accounts", { status: "ACTIVE", page: 0, size: 1 }),
        getPagedCount("/v1/catalog/services", { isActive: true, page: 0, size: 1 }),
        getPagedCount("/v1/admin/rooms", { statusCode: "AVAILABLE", page: 0, size: 1 }),
        api.get("/v1/admin/reports/summary", {
            params: { fromDate, toDate, reportType: "REVENUE" },
        }),
    ]);

    const report = getApiData<ReportSummaryLike>(reportResponse);

    return {
        activeAccounts,
        activeServices,
        availableRooms,
        monthlyRevenueVnd: Number(report.totalRevenueVnd ?? 0),
        monthlyInvoiceCount: Number(report.totalInvoiceCount ?? 0),
    };
}

export function emptyDashboardSummary(): DashboardSummary {
    return {
        activeAccounts: 0,
        activeServices: 0,
        availableRooms: 0,
        monthlyRevenueVnd: 0,
        monthlyInvoiceCount: 0,
    };
}

export function formatDashboardRevenue(value: number) {
    return value > 0 ? formatCompactVnd(value) : "0 ₫";
}
