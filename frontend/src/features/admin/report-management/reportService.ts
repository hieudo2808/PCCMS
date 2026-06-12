import api, { getApiData } from "~/api/api";
import type { ReportRecord, ReportSearchParams, ReportSummary } from "./types";

interface BackendReportSummary {
    totalRevenueVnd?: number | string;
    totalInvoiceCount?: number;
    items?: BackendRevenueRow[];
}

interface BackendRevenueRow {
    reportDate?: string;
    categoryCode?: string;
    serviceId?: string;
    serviceName?: string;
    revenueVnd?: number | string;
    invoiceCount?: number;
}

export function formatIsoDate(date: Date) {
    return date.toISOString().slice(0, 10);
}

export function startOfCurrentMonth() {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 1);
}

export function endOfCurrentMonth() {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth() + 1, 0);
}

export function getDefaultReportFilters(): ReportSearchParams {
    return {
        fromDate: formatIsoDate(startOfCurrentMonth()),
        toDate: formatIsoDate(endOfCurrentMonth()),
        reportType: "REVENUE",
        group: "ALL",
        serviceId: "",
    };
}

export function formatCurrency(value: number) {
    return `${value.toLocaleString("vi-VN")} ₫`;
}

export function formatCompactVnd(value: number) {
    if (value >= 1_000_000) {
        const million = value / 1_000_000;
        const label = Number.isInteger(million) ? String(million) : million.toFixed(1);
        return `${label}M ₫`;
    }
    return formatCurrency(value);
}

function categoryLabel(categoryCode?: string) {
    switch (categoryCode) {
        case "MEDICAL":
            return "Khám bệnh";
        case "GROOMING":
            return "Làm đẹp";
        case "BOARDING":
            return "Lưu trú";
        case "OTHER":
            return "Khác";
        default:
            return "Khác";
    }
}

function toRecord(row: BackendRevenueRow, index: number): ReportRecord {
    const category = categoryLabel(row.categoryCode);
    return {
        id: `${row.reportDate ?? "report"}-${row.serviceId ?? index}`,
        date: row.reportDate ?? "",
        reportType: "REVENUE",
        group: category,
        categoryCode: row.categoryCode ?? "OTHER",
        serviceId: row.serviceId ?? "",
        serviceName: row.serviceName ?? "Không xác định",
        count: Number(row.invoiceCount ?? 0),
        revenue: Number(row.revenueVnd ?? 0),
        note: category,
    };
}

export function buildReportSummary(
    records: ReportRecord[],
    params: ReportSearchParams,
    totalRevenueVnd?: number,
    totalInvoiceCount?: number
): ReportSummary {
    const totalCount = totalInvoiceCount ?? records.reduce((sum, record) => sum + record.count, 0);
    const totalRevenue = totalRevenueVnd ?? records.reduce((sum, record) => sum + record.revenue, 0);
    return {
        reportType: "Doanh thu",
        periodLabel: `${params.fromDate} - ${params.toDate}`,
        totalCount,
        totalRevenue,
        totalValueLabel: formatCurrency(totalRevenue),
    };
}

export async function getReportData(params: ReportSearchParams) {
    const response = await api.get("/v1/admin/reports/summary", {
        params: {
            fromDate: params.fromDate,
            toDate: params.toDate,
            reportType: params.reportType || "REVENUE",
            categoryCode: params.group === "ALL" ? undefined : params.group,
            serviceId: params.serviceId || undefined,
        },
    });
    const summary = getApiData<BackendReportSummary>(response);
    const records = (summary.items ?? []).map(toRecord);
    return {
        records,
        summary: buildReportSummary(
            records,
            params,
            Number(summary.totalRevenueVnd ?? 0),
            Number(summary.totalInvoiceCount ?? 0)
        ),
    };
}

export function getGroupBreakdown(records: ReportRecord[]) {
    const groups = records.reduce<Record<string, { count: number; revenue: number }>>((acc, record) => {
        if (!acc[record.group]) {
            acc[record.group] = { count: 0, revenue: 0 };
        }
        acc[record.group].count += record.count;
        acc[record.group].revenue += record.revenue;
        return acc;
    }, {});

    return Object.entries(groups).map(([group, value]) => ({ group, ...value }));
}
