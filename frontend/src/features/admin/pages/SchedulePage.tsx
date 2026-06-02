import { Card, RequestSwap } from "~/components/molecules";

export function SchedulePage() {
    return (
        <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
            <Card title="Lịch làm việc nhân sự">
                <div className="grid grid-cols-7 gap-3 text-center text-sm">
                    {["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((d) => (
                        <div
                            key={d}
                            className="rounded-2xl bg-slate-100 px-2 py-3 font-medium text-slate-600"
                        >
                            {d}
                        </div>
                    ))}
                    {Array.from({ length: 28 }).map((_, i) => (
                        <div
                            key={i}
                            className="min-h-24 rounded-2xl border border-slate-200 p-2 text-left transition hover:border-emerald-300"
                        >
                            <p className="text-xs text-slate-500">{i + 1}</p>
                            {i % 4 === 0 && (
                                <div className="mt-2 rounded-xl bg-emerald-50 px-2 py-1 text-xs text-emerald-700">
                                    BS An • Sáng
                                </div>
                            )}
                            {i % 6 === 0 && (
                                <div className="mt-2 rounded-xl bg-sky-50 px-2 py-1 text-xs text-sky-700">
                                    Lễ tân Hà • Chiều
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            </Card>
            <Card title="Yêu cầu đổi ca">
                <div className="space-y-3">
                    <RequestSwap
                        who="Lễ tân Hà"
                        from="24/05 • Ca chiều"
                        to="25/05 • Ca sáng"
                        status="Chờ duyệt"
                    />
                    <RequestSwap
                        who="BS An"
                        from="26/05 • Ca sáng"
                        to="27/05 • Ca sáng"
                        status="Đã duyệt"
                    />
                </div>
            </Card>
        </div>
    );
}
