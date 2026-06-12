interface ReportBarChartProps {
    data: Array<{ label: string; value: number }>;
}

export function ReportBarChart({ data }: ReportBarChartProps) {
    const max = Math.max(...data.map((item) => item.value), 1);

    return (
        <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4">
                <h3 className="text-base font-semibold text-slate-900">Biểu đồ cột</h3>
                <p className="text-sm text-slate-500">So sánh lượt hoặc doanh thu theo ngày</p>
            </div>
            <div className="flex h-72 items-end gap-3 rounded-3xl bg-slate-50 p-5">
                {data.length > 0 ? (
                    data.map((item) => (
                        <div key={item.label} className="flex flex-1 flex-col items-center gap-2">
                            <div className="flex h-56 w-full items-end">
                                <div
                                    className="w-full rounded-t-2xl bg-emerald-500/80 transition-all hover:bg-emerald-600"
                                    style={{ height: `${Math.max((item.value / max) * 100, 8)}%` }}
                                />
                            </div>
                            <p className="text-[11px] text-slate-500">{item.label}</p>
                        </div>
                    ))
                ) : (
                    <div className="flex h-full w-full items-center justify-center text-sm text-slate-500">Chưa có dữ liệu biểu đồ</div>
                )}
            </div>
        </div>
    );
}
