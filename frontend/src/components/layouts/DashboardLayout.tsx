import { useEffect, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { PawPrint, ChevronLeft, ChevronRight, Menu, X } from "lucide-react";
import { cx } from "~/utils/cx";
import { roles } from "~/constants/roles";
import { screenMeta } from "~/constants/screenMeta";
import type { RoleKey, ScreenKey } from "~/types/navigation";
import { useAuth } from "~/features/auth/context/AuthContext";
import { NotificationBell } from "~/shared/notifications";

function resolveRole(pathname: string): RoleKey {
    if (pathname.startsWith("/owner")) return "owner";
    if (pathname.startsWith("/staff")) return "staff";
    if (pathname.startsWith("/veterinarian")) return "veterinarian";
    if (pathname.startsWith("/admin")) return "admin";
    return "owner";
}

const roleNames: Record<RoleKey, string> = {
    public: "Khách",
    owner: "Chủ nuôi",
    staff: "Lễ tân",
    admin: "Quản trị viên",
    veterinarian: "Bác sĩ",
};

export function DashboardLayout() {
    const { pathname } = useLocation();
    const navigate = useNavigate();
    const role = resolveRole(pathname);
    const screens = roles[role].screens;

    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
    const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
    const { user, logout } = useAuth();

    useEffect(() => {
        setMobileSidebarOpen(false);
    }, [pathname]);

    useEffect(() => {
        if (!mobileSidebarOpen) return;

        const previousOverflow = document.body.style.overflow;
        const closeOnEscape = (event: KeyboardEvent) => {
            if (event.key === "Escape") setMobileSidebarOpen(false);
        };
        const closeOnDesktopResize = () => {
            if (window.innerWidth >= 768) setMobileSidebarOpen(false);
        };

        document.body.style.overflow = "hidden";
        window.addEventListener("keydown", closeOnEscape);
        window.addEventListener("resize", closeOnDesktopResize);

        return () => {
            document.body.style.overflow = previousOverflow;
            window.removeEventListener("keydown", closeOnEscape);
            window.removeEventListener("resize", closeOnDesktopResize);
        };
    }, [mobileSidebarOpen]);

    const currentScreen = Object.entries(screenMeta).find(
        ([, meta]) => meta.path === pathname
    )?.[0] as ScreenKey | undefined;
    const title = pathname.endsWith("/notifications")
        ? "Thông báo"
        : currentScreen ? screenMeta[currentScreen].label : "Trang chủ";

    return (
        <div className="flex min-h-screen bg-surface">
            {mobileSidebarOpen && (
                <button
                    type="button"
                    aria-label="Đóng menu điều hướng"
                    className="fixed inset-0 z-[35] bg-slate-950/40 md:hidden print:hidden"
                    onClick={() => setMobileSidebarOpen(false)}
                />
            )}

            <aside
                id="dashboard-sidebar"
                aria-label="Điều hướng chính"
                className={cx(
                    "fixed bottom-0 left-0 top-0 z-40 flex w-[min(82vw,280px)] flex-col border-r border-border-main bg-white shadow-xl transition-[width,translate,visibility] duration-300 md:visible md:translate-x-0 md:shadow-none print:hidden",
                    mobileSidebarOpen ? "visible translate-x-0" : "invisible -translate-x-full",
                    sidebarCollapsed ? "md:w-[72px]" : "md:w-[260px]"
                )}
            >
                <div className="flex h-16 shrink-0 items-center gap-3 border-b border-border-main px-4">
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary-600">
                        <PawPrint className="h-5 w-5 text-white" />
                    </div>
                    {(!sidebarCollapsed || mobileSidebarOpen) && (
                        <div className="overflow-hidden">
                            <p className="truncate text-[15px] font-bold tracking-tight text-text-main">
                                Pawluna
                            </p>
                            <p className="truncate text-[11px] font-medium text-text-muted">
                                Quản lý chăm sóc thú cưng
                            </p>
                        </div>
                    )}
                    <button
                        type="button"
                        aria-label="Đóng menu điều hướng"
                        className="ml-auto rounded-lg p-2 text-text-muted transition hover:bg-slate-100 hover:text-text-main md:hidden"
                        onClick={() => setMobileSidebarOpen(false)}
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <nav className="flex-1 overflow-y-auto px-3 py-4 scrollbar-hide">
                    {(!sidebarCollapsed || mobileSidebarOpen) && (
                        <div className="mb-6 px-2 text-xs font-semibold uppercase tracking-wider text-text-muted">
                            {roleNames[role]}
                        </div>
                    )}

                    <div className="space-y-1">
                        {screens.map((screen) => {
                            const meta = screenMeta[screen];
                            const isActive = pathname === meta.path;
                            const Icon = meta.icon;

                            return (
                                <button
                                    key={screen}
                                    onClick={() => {
                                        navigate(meta.path);
                                        setMobileSidebarOpen(false);
                                    }}
                                    aria-current={isActive ? "page" : undefined}
                                    title={sidebarCollapsed ? meta.label : undefined}
                                    className={cx(
                                        "flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-[14px] font-medium transition-all",
                                        isActive
                                            ? "bg-primary-50 text-primary-700"
                                            : "text-text-muted hover:bg-slate-50 hover:text-text-main"
                                    )}
                                >
                                    <Icon className="h-5 w-5 shrink-0" />
                                    {(!sidebarCollapsed || mobileSidebarOpen) && (
                                        <span className="truncate">{meta.label}</span>
                                    )}
                                </button>
                            );
                        })}
                    </div>
                </nav>

                <div className="border-t border-border-main p-3 md:hidden">
                    <p className="truncate px-2 text-sm font-semibold text-text-main">
                        {user?.fullName || "User"}
                    </p>
                    <button
                        type="button"
                        onClick={logout}
                        className="mt-1 w-full rounded-lg px-2 py-2 text-left text-sm font-medium text-red-600 transition hover:bg-red-50"
                    >
                        Đăng xuất
                    </button>
                </div>

                <div className="hidden border-t border-border-main p-3 md:block">
                    <button
                        type="button"
                        aria-label={sidebarCollapsed ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng"}
                        onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
                        className="flex w-full items-center justify-center rounded-xl p-2 text-text-muted transition hover:bg-slate-100 hover:text-text-main"
                    >
                        {sidebarCollapsed ? (
                            <ChevronRight className="h-5 w-5" />
                        ) : (
                            <ChevronLeft className="h-5 w-5" />
                        )}
                    </button>
                </div>
            </aside>

            <div
                className={cx(
                    "flex min-w-0 flex-1 flex-col transition-all duration-300 print:ml-0",
                    sidebarCollapsed ? "md:ml-[72px]" : "md:ml-[260px]"
                )}
            >
                <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center justify-between gap-2 border-b border-border-main bg-white/80 px-3 backdrop-blur-md sm:px-6 print:hidden">
                    <div className="flex min-w-0 items-center gap-2 sm:gap-3">
                        <button
                            type="button"
                            aria-label={mobileSidebarOpen ? "Đóng menu điều hướng" : "Mở menu điều hướng"}
                            aria-expanded={mobileSidebarOpen}
                            aria-controls="dashboard-sidebar"
                            className="shrink-0 rounded-xl p-2 text-text-muted transition hover:bg-slate-100 md:hidden"
                            onClick={() => setMobileSidebarOpen((open) => !open)}
                        >
                            {mobileSidebarOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
                        </button>
                        <div className="min-w-0">
                            <h1 className="truncate text-[16px] font-semibold tracking-tight text-text-main sm:text-[18px]">
                                {title}
                            </h1>
                        </div>
                    </div>

                    <div className="flex shrink-0 items-center gap-1 sm:gap-3">
                        <NotificationBell role={role} />

                        <div className="hidden h-6 w-px bg-border-main sm:block" />

                        <div className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-full bg-slate-900 text-sm font-semibold text-white transition hover:bg-slate-700">
                            {user?.fullName?.charAt(0)?.toUpperCase() || "U"}
                        </div>
                        <div className="mr-2 hidden flex-col text-sm sm:flex">
                            <span className="font-semibold text-text-main">
                                {user?.fullName || "User"}
                            </span>
                            <button
                                onClick={logout}
                                className="text-xs text-red-500 hover:text-red-700 text-left"
                            >
                                Đăng xuất
                            </button>
                        </div>
                    </div>
                </header>

                <main className="min-w-0 flex-1 p-4 md:p-6 lg:p-8">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
