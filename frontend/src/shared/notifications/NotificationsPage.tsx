import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Archive, Check, CheckCheck, ChevronLeft, ChevronRight, Inbox, LoaderCircle } from "lucide-react";
import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { notificationApi, notificationKeys, type NotificationResponse, type NotificationStatus } from "~/shared/api/notificationApi";
import type { RoleKey } from "~/types/navigation";
import { cx } from "~/utils/cx";
import { notificationTarget } from "./notificationNavigation";

type Tab = "ALL" | "UNREAD" | "ARCHIVED";
const tabs: { value: Tab; label: string }[] = [
    { value: "ALL", label: "Tất cả" },
    { value: "UNREAD", label: "Chưa đọc" },
    { value: "ARCHIVED", label: "Đã lưu trữ" },
];

function roleFromPath(pathname: string): RoleKey {
    return (pathname.split("/")[1] || "owner") as RoleKey;
}

export function NotificationsPage() {
    const [tab, setTab] = useState<Tab>("ALL");
    const [page, setPage] = useState(0);
    const { pathname } = useLocation();
    const role = roleFromPath(pathname);
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const params = { page, size: 20, status: tab === "ALL" ? undefined : tab as NotificationStatus };
    const notificationsQuery = useQuery({
        queryKey: notificationKeys.list(params),
        queryFn: () => notificationApi.listMyNotifications(params),
    });
    const refresh = () => queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    const readMutation = useMutation({ mutationFn: notificationApi.markRead, onSuccess: refresh });
    const archiveMutation = useMutation({ mutationFn: notificationApi.archive, onSuccess: refresh });
    const readAllMutation = useMutation({ mutationFn: notificationApi.markAllRead, onSuccess: refresh });

    const openNotification = async (notification: NotificationResponse) => {
        if (notification.statusCode === "UNREAD") await readMutation.mutateAsync(notification.id);
        navigate(notificationTarget(notification, role));
    };

    return (
        <div className="mx-auto max-w-5xl space-y-5">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                    <h2 className="text-2xl font-bold tracking-tight text-text-main">Thông báo</h2>
                    <p className="mt-1 text-sm text-text-muted">Theo dõi các cập nhật quan trọng từ phòng khám.</p>
                </div>
                <button
                    type="button"
                    onClick={() => readAllMutation.mutate()}
                    disabled={readAllMutation.isPending}
                    className="inline-flex items-center justify-center gap-2 rounded-lg border border-border-main bg-white px-4 py-2 text-sm font-semibold text-text-main transition hover:bg-slate-50 disabled:opacity-50"
                >
                    <CheckCheck className="h-4 w-4" /> Đánh dấu tất cả đã đọc
                </button>
            </div>

            <div className="overflow-hidden rounded-xl border border-border-main bg-white">
                <div className="flex gap-1 overflow-x-auto border-b border-border-main px-3 pt-2" role="tablist" aria-label="Lọc thông báo">
                    {tabs.map((item) => (
                        <button
                            key={item.value}
                            type="button"
                            role="tab"
                            aria-selected={tab === item.value}
                            onClick={() => { setTab(item.value); setPage(0); }}
                            className={cx(
                                "whitespace-nowrap border-b-2 px-4 py-2.5 text-sm font-semibold transition",
                                tab === item.value ? "border-primary-600 text-primary-700" : "border-transparent text-text-muted hover:text-text-main"
                            )}
                        >
                            {item.label}
                        </button>
                    ))}
                </div>

                {notificationsQuery.isLoading && (
                    <div className="flex min-h-52 items-center justify-center gap-2 text-sm text-text-muted"><LoaderCircle className="h-5 w-5 animate-spin" /> Đang tải thông báo</div>
                )}
                {notificationsQuery.isError && (
                    <div className="min-h-52 p-10 text-center text-sm text-red-600">Không thể tải thông báo. <button type="button" className="font-semibold underline" onClick={() => notificationsQuery.refetch()}>Thử lại</button></div>
                )}
                {!notificationsQuery.isLoading && !notificationsQuery.isError && (notificationsQuery.data?.content?.length || 0) === 0 && (
                    <div className="flex min-h-52 flex-col items-center justify-center p-8 text-center text-text-muted"><Inbox className="mb-3 h-10 w-10 text-slate-300" /><p className="font-medium">Không có thông báo trong mục này.</p></div>
                )}

                <div className="divide-y divide-slate-100">
                    {notificationsQuery.data?.content?.map((notification) => (
                        <article key={notification.id} className={cx("flex gap-3 p-4 transition hover:bg-slate-50 sm:p-5", notification.statusCode === "UNREAD" && "bg-primary-50/50")}>
                            <button type="button" onClick={() => void openNotification(notification)} className="min-w-0 flex-1 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500">
                                <span className="flex items-start gap-3">
                                    <span className={cx("mt-2 h-2.5 w-2.5 shrink-0 rounded-full", notification.statusCode === "UNREAD" ? "bg-primary-600" : "bg-slate-200")} />
                                    <span className="min-w-0">
                                        <span className="block font-semibold text-text-main">{notification.title}</span>
                                        <span className="mt-1 block text-sm leading-6 text-text-muted">{notification.body}</span>
                                        <time className="mt-2 block text-xs text-slate-400">{new Date(notification.createdAt).toLocaleString("vi-VN")}</time>
                                    </span>
                                </span>
                            </button>
                            <div className="flex shrink-0 items-start gap-1">
                                {notification.statusCode === "UNREAD" && (
                                    <button type="button" title="Đánh dấu đã đọc" aria-label="Đánh dấu đã đọc" onClick={() => readMutation.mutate(notification.id)} className="rounded-lg p-2 text-text-muted hover:bg-white hover:text-primary-700"><Check className="h-4 w-4" /></button>
                                )}
                                {notification.statusCode !== "ARCHIVED" && (
                                    <button type="button" title="Lưu trữ" aria-label="Lưu trữ thông báo" onClick={() => archiveMutation.mutate(notification.id)} className="rounded-lg p-2 text-text-muted hover:bg-white hover:text-text-main"><Archive className="h-4 w-4" /></button>
                                )}
                            </div>
                        </article>
                    ))}
                </div>

                {(notificationsQuery.data?.totalPages ?? 0) > 1 && (
                    <div className="flex items-center justify-between border-t border-border-main px-4 py-3 text-sm">
                        <span className="text-text-muted">Trang {page + 1} / {notificationsQuery.data?.totalPages}</span>
                        <div className="flex gap-2">
                            <button type="button" aria-label="Trang trước" disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="rounded-lg border border-border-main p-2 disabled:opacity-40"><ChevronLeft className="h-4 w-4" /></button>
                            <button type="button" aria-label="Trang sau" disabled={page + 1 >= (notificationsQuery.data?.totalPages ?? 0)} onClick={() => setPage((value) => value + 1)} className="rounded-lg border border-border-main p-2 disabled:opacity-40"><ChevronRight className="h-4 w-4" /></button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
