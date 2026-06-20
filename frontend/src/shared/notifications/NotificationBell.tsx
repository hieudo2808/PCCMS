import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck, LoaderCircle } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    notificationApi,
    notificationKeys,
    type NotificationResponse,
} from "~/shared/api/notificationApi";
import type { RoleKey } from "~/types/navigation";
import { cx } from "~/utils/cx";
import { notificationCenterPath, notificationTarget } from "./notificationNavigation";

function createdAtLabel(value: string) {
    return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(new Date(value));
}

export function NotificationBell({ role }: { role: RoleKey }) {
    const [open, setOpen] = useState(false);
    const rootRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const countQuery = useQuery({
        queryKey: notificationKeys.unreadCount(),
        queryFn: notificationApi.getUnreadCount,
    });
    const listParams = { page: 0, size: 5 } as const;
    const listQuery = useQuery({
        queryKey: notificationKeys.list(listParams),
        queryFn: () => notificationApi.listMyNotifications(listParams),
        enabled: open,
    });

    const refresh = () => queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    const readMutation = useMutation({ mutationFn: notificationApi.markRead, onSuccess: refresh });
    const readAllMutation = useMutation({ mutationFn: notificationApi.markAllRead, onSuccess: refresh });

    useEffect(() => {
        if (!open) return;
        const close = (event: MouseEvent) => {
            if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
        };
        const closeOnEscape = (event: KeyboardEvent) => {
            if (event.key === "Escape") setOpen(false);
        };
        document.addEventListener("mousedown", close);
        document.addEventListener("keydown", closeOnEscape);
        return () => {
            document.removeEventListener("mousedown", close);
            document.removeEventListener("keydown", closeOnEscape);
        };
    }, [open]);

    const openNotification = async (notification: NotificationResponse) => {
        if (notification.statusCode === "UNREAD") await readMutation.mutateAsync(notification.id);
        setOpen(false);
        navigate(notificationTarget(notification, role));
    };

    const unreadCount = countQuery.data?.unreadCount ?? 0;

    return (
        <div ref={rootRef} className="relative">
            <button
                type="button"
                aria-label={`Thông báo${unreadCount ? `, ${unreadCount} chưa đọc` : ""}`}
                aria-expanded={open}
                aria-haspopup="dialog"
                onClick={() => setOpen((value) => !value)}
                className="relative rounded-full p-2 text-text-muted transition hover:bg-slate-100 hover:text-text-main focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500"
            >
                <Bell className="h-5 w-5" />
                {unreadCount > 0 && (
                    <span className="absolute -right-1 -top-1 min-w-5 rounded-full bg-red-600 px-1 text-center text-[10px] font-bold leading-5 text-white">
                        {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                )}
            </button>

            {open && (
                <section
                    role="dialog"
                    aria-label="Thông báo gần đây"
                    className="fixed inset-x-3 top-16 z-50 overflow-hidden rounded-xl border border-border-main bg-white shadow-xl sm:absolute sm:inset-auto sm:right-0 sm:top-12 sm:w-[380px]"
                >
                    <div className="flex items-center justify-between border-b border-border-main px-4 py-3">
                        <div>
                            <h2 className="font-semibold text-text-main">Thông báo</h2>
                            <p className="text-xs text-text-muted">{unreadCount} thông báo chưa đọc</p>
                        </div>
                        {unreadCount > 0 && (
                            <button
                                type="button"
                                disabled={readAllMutation.isPending}
                                onClick={() => readAllMutation.mutate()}
                                className="flex items-center gap-1 text-xs font-medium text-primary-700 hover:text-primary-800 disabled:opacity-50"
                            >
                                <CheckCheck className="h-4 w-4" /> Đọc tất cả
                            </button>
                        )}
                    </div>

                    <div className="max-h-[min(420px,65vh)] overflow-y-auto">
                        {listQuery.isLoading && (
                            <div className="flex items-center justify-center gap-2 p-8 text-sm text-text-muted">
                                <LoaderCircle className="h-4 w-4 animate-spin" /> Đang tải
                            </div>
                        )}
                        {listQuery.isError && (
                            <div className="p-6 text-center text-sm text-red-600">
                                Không thể tải thông báo.
                                <button type="button" onClick={() => listQuery.refetch()} className="ml-2 font-semibold underline">Thử lại</button>
                            </div>
                        )}
                        {!listQuery.isLoading && !listQuery.isError && (listQuery.data?.content?.length || 0) === 0 && (
                            <p className="p-8 text-center text-sm text-text-muted">Bạn chưa có thông báo nào.</p>
                        )}
                        {listQuery.data?.content?.map((notification) => (
                            <button
                                type="button"
                                key={notification.id}
                                onClick={() => void openNotification(notification)}
                                className={cx(
                                    "block w-full border-b border-slate-100 px-4 py-3 text-left transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary-500",
                                    notification.statusCode === "UNREAD" && "bg-primary-50/60"
                                )}
                            >
                                <span className="flex items-start gap-3">
                                    <span className={cx("mt-2 h-2 w-2 shrink-0 rounded-full", notification.statusCode === "UNREAD" ? "bg-primary-600" : "bg-transparent")} />
                                    <span className="min-w-0">
                                        <span className="block text-sm font-semibold text-text-main">{notification.title}</span>
                                        <span className="mt-1 line-clamp-2 block text-sm text-text-muted">{notification.body}</span>
                                        <span className="mt-1.5 block text-xs text-slate-400">{createdAtLabel(notification.createdAt)}</span>
                                    </span>
                                </span>
                            </button>
                        ))}
                    </div>

                    <button
                        type="button"
                        onClick={() => { setOpen(false); navigate(notificationCenterPath(role)); }}
                        className="w-full px-4 py-3 text-sm font-semibold text-primary-700 transition hover:bg-slate-50"
                    >
                        Xem tất cả thông báo
                    </button>
                </section>
            )}
        </div>
    );
}
