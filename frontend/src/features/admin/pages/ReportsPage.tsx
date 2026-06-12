import { useEffect, useMemo, useState } from "react";
import { Button, Input, Select } from "~/components/atoms";
import { Card } from "~/components/molecules";
import { ReportBarChart } from "../report-management/components/ReportBarChart";
import { ReportEmptyState } from "../report-management/components/ReportEmptyState";
import { ReportPieChart } from "../report-management/components/ReportPieChart";
import { ReportSummaryCards } from "../report-management/components/ReportSummaryCards";
import { ReportTable } from "../report-management/components/ReportTable";
import { getDefaultReportFilters, getGroupBreakdown, getReportData } from "../report-management/reportService";
import type { ReportGroup, ReportRecord, ReportSearchParams, ReportSummary } from "../report-management/types";

const revenueReportLabel = "Doanh thu";

const groupOptions: Array<{ value: ReportGroup; label: string }> = [
    { value: "ALL", label: "Tất cả" },
    { value: "MEDICAL", label: "Khám bệnh" },
    { value: "GROOMING", label: "Làm đẹp" },
    { value: "BOARDING", label: "Lưu trú" },
    { value: "OTHER", label: "Khác" },
];

const emptyFilters: ReportSearchParams = {
    fromDate: "",
    toDate: "",
    reportType: "REVENUE",
    group: "ALL",
    serviceId: "",
};

function getGroupLabel(value: ReportGroup) {
    return groupOptions.find((item) => item.value === value)?.label ?? "Tất cả";
}

function getGroupValue(label: string): ReportGroup {
    return groupOptions.find((item) => item.label === label)?.value ?? "ALL";
}

export function ReportsPage() {
    const [filters, setFilters] = useState<ReportSearchParams>(getDefaultReportFilters());
    const [records, setRecords] = useState<ReportRecord[]>([]);
    const [summary, setSummary] = useState<ReportSummary | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [emptyMessage, setEmptyMessage] = useState("");

    const chartData = useMemo(
        () => records.slice(0, 7).map((record) => ({ label: record.date, value: record.revenue || record.count })),
        [records]
    );

    const pieData = useMemo(() => {
        const groups = getGroupBreakdown(records);
        const total = groups.reduce((sum, item) => sum + item.revenue, 0) || 1;
        return groups.map((item) => ({
            label: item.group,
            value: item.revenue,
            percent: Math.round((item.revenue / total) * 100),
        }));
    }, [records]);

    const loadReport = async (params: ReportSearchParams) => {
        setLoading(true);
        setError("");
        setEmptyMessage("");
        try {
            const data = await getReportData(params);
            setRecords(data.records);
            setSummary(data.summary);
            if (data.records.length === 0) {
                setEmptyMessage("Không có dữ liệu trong khoảng thời gian đã chọn");
            }
        } catch {
            setRecords([]);
            setSummary(null);
            setError("Không thể tổng hợp dữ liệu từ hệ thống");
            setEmptyMessage("Không có dữ liệu trong khoảng thời gian đã chọn");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadReport(getDefaultReportFilters());
    }, []);

    const onSearch = async () => {
        if (!filters.fromDate || !filters.toDate || !filters.reportType) {
            setError("Vui lòng chọn đầy đủ khoảng thời gian và loại báo cáo");
            return;
        }
        if (filters.toDate < filters.fromDate) {
            setError("Đến ngày phải lớn hơn hoặc bằng Từ ngày");
            return;
        }
        await loadReport(filters);
    };

    const onReset = () => {
        const defaults = getDefaultReportFilters();
        setFilters(defaults);
        void loadReport(defaults);
    };

    return (
        <div className="space-y-6 print:space-y-4">
            <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-semibold text-slate-900">Báo cáo thống kê</h1>
                        <p className="text-sm text-slate-500">Tổng hợp dữ liệu vận hành, dịch vụ và doanh thu theo tiêu chí thống kê.</p>
                    </div>
                    <div className="print:hidden">
                        <Button
                            variant="primary"
                            onClick={() => window.print()}
                            disabled={records.length === 0 || loading}
                        >
                            Xuất Báo Cáo PDF
                        </Button>
                    </div>
                </div>
            </div>

            <div className="print:hidden">
                <Card title="Tiêu chí thống kê" subtitle="Có thể lọc báo cáo theo nhóm dịch vụ: Khám bệnh, Làm đẹp, Lưu trú hoặc Khác.">
                <div className="grid gap-4 lg:grid-cols-5">
                    <Input
                        label="Từ ngày"
                        type="date"
                        value={filters.fromDate}
                        onChange={(e) => setFilters({ ...filters, fromDate: e.target.value })}
                    />
                    <Input
                        label="Đến ngày"
                        type="date"
                        value={filters.toDate}
                        onChange={(e) => setFilters({ ...filters, toDate: e.target.value })}
                    />
                    <Select
                        label="Loại báo cáo"
                        value={revenueReportLabel}
                        onChange={() => setFilters({ ...filters, reportType: "REVENUE" })}
                        options={[revenueReportLabel]}
                    />
                    <Select
                        label="Nhóm dịch vụ"
                        value={getGroupLabel(filters.group)}
                        onChange={(e) => setFilters({ ...filters, group: getGroupValue(e.target.value) })}
                        options={groupOptions.map((item) => item.label)}
                    />
                    <Input
                        label="Mã dịch vụ"
                        value={filters.serviceId}
                        onChange={(e) => setFilters({ ...filters, serviceId: e.target.value })}
                        placeholder="Tùy chọn"
                    />
                </div>
                {error && <p className="mt-3 text-sm font-medium text-error-600">{error}</p>}
                <div className="mt-5 flex flex-wrap gap-3">
                    <Button onClick={onSearch} disabled={loading}>
                        {loading ? "Đang tổng hợp..." : "Xem báo cáo"}
                    </Button>
                    <Button variant="outline" onClick={onReset} disabled={loading}>
                        Làm mới
                    </Button>
                    <Button variant="ghost" onClick={() => setFilters(emptyFilters)} disabled={loading}>
                        Xóa tiêu chí
                    </Button>
                </div>
            </Card>
            </div>

            <ReportSummaryCards summary={summary} />

            {emptyMessage ? (
                <ReportEmptyState message={emptyMessage} />
            ) : (
                <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
                    <ReportBarChart data={chartData} />
                    <ReportPieChart items={pieData} />
                </div>
            )}

            {records.length > 0 && <ReportTable items={records} />}
        </div>
    );
}
