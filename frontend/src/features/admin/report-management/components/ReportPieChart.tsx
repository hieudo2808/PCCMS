interface ReportPieChartProps {
    items: Array<{ label: string; value: number; percent: number }>;
}

const colors = ["bg-emerald-500", "bg-sky-500", "bg-amber-500", "bg-rose-500", "bg-violet-500"];

export function ReportPieChart({ items }: ReportPieChartProps) {
    return (
        <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4">
                <h3 className="text-base font-semibold text-slate-900">Tỷ trọng theo nhóm</h3>
                <p className="text-sm text-slate-500">Tỷ lệ doanh thu hoặc số lượt theo nhóm dữ liệu</p>
            </div>
            <div className="space-y-3">
                {items.length > 0 ? (
                    items.map((item, index) => (
                        <div key={item.label}>
                            <div className="mb-1 flex items-center justify-between text-sm">
                                <span className="font-medium text-slate-700">{item.label}</span>
                                <span className="text-slate-500">{item.percent}%</span>
                            </div>
                            <div className="h-3 rounded-full bg-slate-100">
                                <div className={`h-3 rounded-full ${colors[index % colors.length]}`} style={{ width: `${item.percent}%` }} />
                            </div>
                        </div>
                    ))
                ) : (
                    <div className="rounded-2xl border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500">
                        Chưa có dữ liệu tỷ trọng
                    </div>
                )}
            </div>
        </div>
    );
}
