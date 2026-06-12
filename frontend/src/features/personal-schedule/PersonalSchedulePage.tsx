import { useEffect, useMemo, useState } from "react";
import { Button, Input, Tag, Textarea, AutocompleteInput } from "~/components/atoms";
import { Card, EmptyState, SummaryRow } from "~/components/molecules";
import {
    cancelMyShiftChangeRequest,
    createMyShiftChangeRequest,
    getMyShiftChangeRequests,
    getIncomingShiftChangeRequests,
    respondToIncomingShiftChangeRequest,
    getShiftTargetStaffOptions,
    getMyWorkSchedules,
    type PersonalScheduleItem,
    type ShiftChangeRequestItem,
    type ShiftTargetStaffOption,
} from "./personalScheduleService";

type PersonalScheduleViewMode = "Ngày" | "Tuần" | "Tháng";

interface PersonalSchedulePageProps {
    title?: string;
}

const MAX_SCHEDULE_RANGE_DAYS = 90;

const toIsoDate = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
};

const addDays = (date: Date, days: number) => {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
};

const daysBetween = (fromDate: string, toDate: string) =>
    (new Date(`${toDate}T00:00:00`).getTime() - new Date(`${fromDate}T00:00:00`).getTime()) /
    (24 * 60 * 60 * 1000);

const getWeekRange = () => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = addDays(today, -today.getDay());
    return { fromDate: toIsoDate(start), toDate: toIsoDate(addDays(start, 6)) };
};

const statusTone = (status: string) => {
    if (status === "Đã phân công" || status === "Đã đồng ý") return "green";
    if (status === "Đã hủy" || status === "Từ chối") return "red";
    return "amber";
};

const canRequestChange = (schedule: PersonalScheduleItem) => {
    const today = toIsoDate(new Date());
    return schedule.status === "Đã phân công" && schedule.workDate > today;
};

export function PersonalSchedulePage({ title = "Lịch làm việc cá nhân" }: PersonalSchedulePageProps) {
    const defaultRange = useMemo(getWeekRange, []);
    const [filters, setFilters] = useState({
        viewMode: "Tuần" as PersonalScheduleViewMode,
        fromDate: defaultRange.fromDate,
        toDate: defaultRange.toDate,
    });
    const [items, setItems] = useState<PersonalScheduleItem[]>([]);
    const [requests, setRequests] = useState<ShiftChangeRequestItem[]>([]);
    const [incomingRequests, setIncomingRequests] = useState<ShiftChangeRequestItem[]>([]);
    const [targetStaffOptions, setTargetStaffOptions] = useState<ShiftTargetStaffOption[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [dialogError, setDialogError] = useState("");
    const [feedback, setFeedback] = useState("");
    const [selectedSchedule, setSelectedSchedule] = useState<PersonalScheduleItem | null>(null);
    const [respondRequest, setRespondRequest] = useState<ShiftChangeRequestItem | null>(null);
    const [reason, setReason] = useState("");
    const [respondReason, setRespondReason] = useState("");
    const [targetStaffId, setTargetStaffId] = useState("");

    const loadData = async (fromDate = filters.fromDate, toDate = filters.toDate) => {
        setLoading(true);
        setError("");
        try {
            const [scheduleData, requestData, incomingData, staffOptions] = await Promise.all([
                getMyWorkSchedules(fromDate, toDate),
                getMyShiftChangeRequests(),
                getIncomingShiftChangeRequests(),
                getShiftTargetStaffOptions(),
            ]);
            setItems(scheduleData);
            setRequests(requestData);
            setIncomingRequests(incomingData);
            setTargetStaffOptions(staffOptions);
        } catch {
            setError("Không thể tải lịch làm việc từ hệ thống");
            setItems([]);
            setRequests([]);
            setIncomingRequests([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadData(defaultRange.fromDate, defaultRange.toDate);
    }, [defaultRange.fromDate, defaultRange.toDate]);

    const summary = useMemo(() => {
        const today = toIsoDate(new Date());
        const total = items.length;
        const cancelled = items.filter((item) => item.status === "Đã hủy").length;
        const todayCount = items.filter((item) => item.workDate === today).length;
        const upcoming = items.filter((item) => item.workDate > today && item.status === "Đã phân công").length;
        return { total, todayCount, upcoming, cancelled };
    }, [items]);

    const targetStaffListOptions = useMemo(() => {
        return [
            { id: "", label: "Admin tự điều phối" },
            ...targetStaffOptions
                .filter((staff) => !selectedSchedule || staff.id !== selectedSchedule.userId)
                .map((staff) => ({
                    id: staff.id,
                    label: staff.roleName ? `${staff.fullName} - ${staff.roleName}` : staff.fullName
                }))
        ];
    }, [targetStaffOptions, selectedSchedule]);

    const handleSearch = async () => {
        if (!filters.fromDate || !filters.toDate) {
            setError("Vui lòng chọn đầy đủ khoảng thời gian");
            return;
        }
        if (filters.toDate < filters.fromDate) {
            setError("Đến ngày phải lớn hơn hoặc bằng Từ ngày");
            return;
        }
        if (daysBetween(filters.fromDate, filters.toDate) > MAX_SCHEDULE_RANGE_DAYS) {
            setError(`Khoảng thời gian xem lịch tối đa ${MAX_SCHEDULE_RANGE_DAYS} ngày`);
            return;
        }
        await loadData(filters.fromDate, filters.toDate);
    };

    const handleReset = async () => {
        const range = getWeekRange();
        setFilters({ viewMode: "Tuần", ...range });
        setFeedback("");
        await loadData(range.fromDate, range.toDate);
    };

    const openRequestDialog = (schedule: PersonalScheduleItem) => {
        if (!canRequestChange(schedule)) {
            setDialogError(
                schedule.status === "Đã hủy"
                    ? "Ca làm việc đã hủy nên không thể yêu cầu đổi"
                    : "Chỉ có thể yêu cầu đổi ca đã phân công trong tương lai"
            );
            return;
        }
        setDialogError("");
        setSelectedSchedule(schedule);
        setReason("");
        setTargetStaffId("");
    };

    const submitShiftChange = async () => {
        if (!selectedSchedule) return;
        if (!reason.trim()) {
            setDialogError("Vui lòng nhập lý do đổi ca");
            return;
        }

        setLoading(true);
        setDialogError("");
        try {
            const request = await createMyShiftChangeRequest(selectedSchedule.id, reason.trim(), targetStaffId);
            setRequests((prev) => [request, ...prev]);
            setSelectedSchedule(null);
            setReason("");
            setTargetStaffId("");
            setFeedback("Gửi yêu cầu đổi ca thành công");
        } catch {
            setDialogError("Không thể gửi yêu cầu đổi ca");
        } finally {
            setLoading(false);
        }
    };

    const cancelRequest = async (requestId: string) => {
        setLoading(true);
        setFeedback("");
        try {
            const updated = await cancelMyShiftChangeRequest(requestId);
            setRequests((prev) => prev.map((item) => (item.requestId === requestId ? updated : item)));
            setFeedback("Đã hủy yêu cầu đổi ca");
        } catch {
            setError("Không thể hủy yêu cầu đổi ca");
        } finally {
            setLoading(false);
        }
    };

    const submitRespond = async (isAccepted: boolean) => {
        if (!respondRequest) return;
        setLoading(true);
        setDialogError("");
        try {
            const updated = await respondToIncomingShiftChangeRequest(respondRequest.requestId, isAccepted, respondReason.trim());
            setIncomingRequests((prev) => prev.map((item) => (item.requestId === updated.requestId ? updated : item)));
            setRespondRequest(null);
            setRespondReason("");
            setFeedback(isAccepted ? "Đã đồng ý đổi ca" : "Đã từ chối đổi ca");
        } catch {
            setDialogError("Không thể phản hồi yêu cầu đổi ca");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-2">
                <h1 className="text-2xl font-semibold text-slate-900">{title}</h1>
                <p className="text-sm text-slate-500">Xem các ca làm việc đã được phân công cho tài khoản hiện tại.</p>
            </div>

            {feedback && <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">{feedback}</div>}
            {error && <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div>}

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                <Card><SummaryRow label="Tổng ca trong khoảng" value={String(summary.total)} /></Card>
                <Card><SummaryRow label="Ca hôm nay" value={String(summary.todayCount)} /></Card>
                <Card><SummaryRow label="Ca sắp tới" value={String(summary.upcoming)} /></Card>
                <Card><SummaryRow label="Ca đã hủy" value={String(summary.cancelled)} /></Card>
            </div>

            <Card title="Bộ lọc thời gian" subtitle="Chọn ngày, tuần hoặc tháng để xem lịch cá nhân">
                <div className="grid gap-4 lg:grid-cols-3">
                    <div className="flex flex-col gap-1.5">
                        <label className="text-[13px] font-medium text-slate-700">Kiểu xem</label>
                        <select
                            className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-[14px] text-slate-900 outline-none"
                            value={filters.viewMode}
                            onChange={(event) => setFilters({ ...filters, viewMode: event.target.value as PersonalScheduleViewMode })}
                        >
                            <option value="Ngày">Ngày</option>
                            <option value="Tuần">Tuần</option>
                            <option value="Tháng">Tháng</option>
                        </select>
                    </div>
                    <Input label="Từ ngày" type="date" value={filters.fromDate} max={filters.toDate} onChange={(event) => setFilters({ ...filters, fromDate: event.target.value })} />
                    <Input label="Đến ngày" type="date" value={filters.toDate} min={filters.fromDate} max={toIsoDate(addDays(new Date(`${filters.fromDate}T00:00:00`), MAX_SCHEDULE_RANGE_DAYS))} onChange={(event) => setFilters({ ...filters, toDate: event.target.value })} />
                </div>
                <div className="mt-5 flex flex-wrap gap-3">
                    <Button onClick={() => void handleSearch()} disabled={loading}>Xem lịch</Button>
                    <Button variant="outline" onClick={() => void handleReset()} disabled={loading}>Làm mới</Button>
                </div>
            </Card>

            {loading && items.length === 0 ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Đang tải lịch làm việc...</div>
            ) : items.length === 0 ? (
                <EmptyState title="Không có lịch làm việc trong khoảng thời gian được chọn" description="Hãy đổi khoảng ngày để xem các ca làm việc khác của tài khoản hiện tại." />
            ) : (
                <Card title="Lịch làm việc cá nhân" subtitle="Danh sách các ca làm việc đã được phân công">
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                            <thead className="bg-slate-50 text-slate-500">
                                <tr>
                                    <th className="px-4 py-3 font-medium">Ngày làm việc</th>
                                    <th className="px-4 py-3 font-medium">Ca làm việc</th>
                                    <th className="px-4 py-3 font-medium">Thời gian</th>
                                    <th className="px-4 py-3 font-medium">Vai trò</th>
                                    <th className="px-4 py-3 font-medium">Ghi chú</th>
                                    <th className="px-4 py-3 font-medium">Trạng thái</th>
                                    <th className="px-4 py-3 font-medium">Hành động</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-200 bg-white">
                                {items.map((item) => {
                                    const disabled = !canRequestChange(item);
                                    return (
                                        <tr key={item.id}>
                                            <td className="px-4 py-3">{item.workDate}</td>
                                            <td className="px-4 py-3">{item.shift}</td>
                                            <td className="px-4 py-3">{item.startTime} - {item.endTime}</td>
                                            <td className="px-4 py-3">{item.role}</td>
                                            <td className="px-4 py-3">{item.note || "-"}</td>
                                            <td className="px-4 py-3"><Tag tone={statusTone(item.status)}>{item.status}</Tag></td>
                                            <td className="px-4 py-3">
                                                <Button variant="outline" className="px-3 py-1.5 text-xs" onClick={() => openRequestDialog(item)} disabled={disabled}>
                                                    Yêu cầu đổi ca
                                                </Button>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </Card>
            )}

            <Card title="Yêu cầu đổi ca đã gửi" subtitle="Theo dõi các yêu cầu đổi ca của tài khoản hiện tại.">
                {requests.length === 0 ? (
                    <EmptyState title="Chưa có yêu cầu đổi ca nào được gửi" description="Các yêu cầu đổi ca bạn gửi sẽ hiển thị tại đây." />
                ) : (
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                            <thead className="bg-slate-50 text-slate-500">
                                <tr>
                                    <th className="px-4 py-3 font-medium">Mã yêu cầu</th>
                                    <th className="px-4 py-3 font-medium">Mã lịch làm việc</th>
                                    <th className="px-4 py-3 font-medium">Lý do</th>
                                    <th className="px-4 py-3 font-medium">Trạng thái</th>
                                    <th className="px-4 py-3 font-medium">Hành động</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-200 bg-white">
                                {requests.map((item) => {
                                    const pending = item.status === "Đang chờ";
                                    return (
                                        <tr key={item.requestId}>
                                            <td className="px-4 py-3 font-medium">{item.requestId}</td>
                                            <td className="px-4 py-3">{item.scheduleId}</td>
                                            <td className="px-4 py-3">{item.reason}</td>
                                            <td className="px-4 py-3"><Tag tone={statusTone(item.status)}>{item.status}</Tag></td>
                                            <td className="px-4 py-3">
                                                {pending ? (
                                                    <Button variant="outline" className="px-3 py-1.5 text-xs" disabled={loading} onClick={() => void cancelRequest(item.requestId)}>
                                                        Hủy yêu cầu
                                                    </Button>
                                                ) : (
                                                    <span className="text-xs text-slate-500">Đã xử lý</span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </Card>

            <Card title="Yêu cầu đổi ca đến tôi" subtitle="Xử lý các yêu cầu đổi ca từ nhân viên khác gửi đến bạn.">
                {incomingRequests.length === 0 ? (
                    <EmptyState title="Chưa có yêu cầu đổi ca nào" description="Bạn sẽ thấy yêu cầu tại đây khi có người muốn đổi ca với bạn." />
                ) : (
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                            <thead className="bg-slate-50 text-slate-500">
                                <tr>
                                    <th className="px-4 py-3 font-medium">Mã yêu cầu</th>
                                    <th className="px-4 py-3 font-medium">Người gửi</th>
                                    <th className="px-4 py-3 font-medium">Mã lịch làm việc</th>
                                    <th className="px-4 py-3 font-medium">Lý do</th>
                                    <th className="px-4 py-3 font-medium">Trạng thái</th>
                                    <th className="px-4 py-3 font-medium">Hành động</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-200 bg-white">
                                {incomingRequests.map((item) => {
                                    const pending = item.status === "Đang chờ";
                                    return (
                                        <tr key={item.requestId}>
                                            <td className="px-4 py-3 font-medium">{item.requestId}</td>
                                            <td className="px-4 py-3 font-medium">{item.senderName}</td>
                                            <td className="px-4 py-3">{item.scheduleId}</td>
                                            <td className="px-4 py-3">{item.reason}</td>
                                            <td className="px-4 py-3"><Tag tone={statusTone(item.status)}>{item.status}</Tag></td>
                                            <td className="px-4 py-3">
                                                {pending ? (
                                                    <Button variant="outline" className="px-3 py-1.5 text-xs" disabled={loading} onClick={() => {
                                                        setRespondRequest(item);
                                                        setRespondReason("");
                                                        setDialogError("");
                                                    }}>
                                                        Phản hồi
                                                    </Button>
                                                ) : (
                                                    <span className="text-xs text-slate-500">Đã xử lý</span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </Card>

            <div className={`fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4 py-6 ${selectedSchedule ? "" : "pointer-events-none opacity-0"}`}>
                {selectedSchedule && (
                    <div className="w-full max-w-2xl">
                        <Card title="Yêu cầu đổi ca" subtitle="Nhập lý do để quản trị viên xử lý yêu cầu đổi ca">
                            <div className="grid gap-4 md:grid-cols-2">
                                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs font-medium text-slate-500">Mã lịch làm việc</p><p className="mt-1 text-sm font-semibold text-slate-900">{selectedSchedule.id}</p></div>
                                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs font-medium text-slate-500">Ngày làm việc</p><p className="mt-1 text-sm font-semibold text-slate-900">{selectedSchedule.workDate}</p></div>
                                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs font-medium text-slate-500">Ca làm việc</p><p className="mt-1 text-sm font-semibold text-slate-900">{selectedSchedule.shift}</p></div>
                                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs font-medium text-slate-500">Thời gian ca</p><p className="mt-1 text-sm font-semibold text-slate-900">{selectedSchedule.startTime} - {selectedSchedule.endTime}</p></div>
                            </div>

                            <div className="mt-5 flex flex-col gap-1.5">
                                <label className="text-[13px] font-medium text-slate-700">Người nhận đổi ca</label>
                                <AutocompleteInput
                                    value={targetStaffId}
                                    onChange={(value) => setTargetStaffId(value)}
                                    options={targetStaffListOptions}
                                    placeholder="Chọn nhân viên hoặc để trống"
                                />
                            </div>

                            <div className="mt-5">
                                <Textarea label="Lý do đổi ca" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Nhập lý do đổi ca" rows={4} />
                            </div>

                            {dialogError && <p className="mt-3 text-sm font-medium text-error-600">{dialogError}</p>}

                            <div className="mt-6 flex flex-wrap justify-end gap-3">
                                <Button variant="outline" onClick={() => setSelectedSchedule(null)} disabled={loading}>Hủy</Button>
                                <Button onClick={() => void submitShiftChange()} disabled={loading}>Gửi yêu cầu</Button>
                            </div>
                        </Card>
                    </div>
                )}
            </div>

            <div className={`fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4 py-6 ${respondRequest ? "" : "pointer-events-none opacity-0"}`}>
                {respondRequest && (
                    <div className="w-full max-w-xl">
                        <Card title="Phản hồi yêu cầu đổi ca" subtitle={`Yêu cầu từ: ${respondRequest.senderName}`}>
                            <div className="grid gap-4 md:grid-cols-2">
                                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs font-medium text-slate-500">Mã lịch làm việc</p><p className="mt-1 text-sm font-semibold text-slate-900">{respondRequest.scheduleId}</p></div>
                                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3"><p className="text-xs font-medium text-slate-500">Lý do của người gửi</p><p className="mt-1 text-sm font-semibold text-slate-900">{respondRequest.reason}</p></div>
                            </div>

                            <div className="mt-5">
                                <Textarea label="Lý do (Tùy chọn)" value={respondReason} onChange={(event) => setRespondReason(event.target.value)} placeholder="Nhập lý do phản hồi..." rows={3} />
                            </div>

                            {dialogError && <p className="mt-3 text-sm font-medium text-error-600">{dialogError}</p>}

                            <div className="mt-6 flex flex-wrap justify-end gap-3">
                                <Button variant="outline" onClick={() => setRespondRequest(null)} disabled={loading}>Hủy</Button>
                                <Button variant="outline" onClick={() => void submitRespond(false)} disabled={loading}>Từ chối</Button>
                                <Button onClick={() => void submitRespond(true)} disabled={loading}>Đồng ý</Button>
                            </div>
                        </Card>
                    </div>
                )}
            </div>
        </div>
    );
}
