import { useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { PawPrint, Bell, ChevronLeft, ChevronRight, Menu } from "lucide-react";
import { cx } from "~/utils/cx";
import { roles } from "~/constants/roles";
import { screenMeta } from "~/constants/screenMeta";
import type { RoleKey, ScreenKey } from "~/types/navigation";

// Utility resolve role từ path
function resolveRole(pathname: string): RoleKey {
    if (pathname.startsWith("/owner")) return "owner";
    if (pathname.startsWith("/reception")) return "reception";
    if (pathname.startsWith("/doctor")) return "doctor";
    if (pathname.startsWith("/admin")) return "admin";
    // Fallback internally
    return "owner";
}

const roleNames: Record<RoleKey, string> = {
    public: "Khách",
    owner: "Chủ nuôi",
    reception: "Lễ tân",
    admin: "Quản trị viên",
    doctor: "Bác sĩ",
};

export function DashboardLayout() {
    const { pathname } = useLocation();
    const navigate = useNavigate();
    const role = resolveRole(pathname);
    const screens = roles[role].screens;

    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

    const currentScreen = Object.entries(screenMeta).find(
        ([, meta]) => meta.path === pathname
    )?.[0] as ScreenKey | undefined;
    const title = currentScreen ? screenMeta[currentScreen].label : "Trang chủ";

    return (
        <div className="flex min-h-screen bg-surface">
            {/* Sidebar */}
            <aside
                className={cx(
                    "fixed bottom-0 left-0 top-0 z-40 hidden flex-col border-r border-border-main bg-white transition-all duration-300 md:flex",
                    sidebarCollapsed ? "w-[72px]" : "w-[260px]"
                )}
            >
                {/* Logo & Brand */}
                <div className="flex h-16 shrink-0 items-center gap-3 border-b border-border-main px-4">
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary-600">
                        <PawPrint className="h-5 w-5 text-white" />
                    </div>
                    {!sidebarCollapsed && (
                        <div className="overflow-hidden">
                            <p className="truncate text-[15px] font-bold tracking-tight text-text-main">
                                Pawluna
                            </p>
                            <p className="truncate text-[11px] font-medium text-text-muted">
                                Quản lý chăm sóc thú cưng
                            </p>
                        </div>
                    )}
                </div>

                {/* Nav */}
                <nav className="flex-1 overflow-y-auto px-3 py-4 scrollbar-hide">
                    {/* User profile brief in nav */}
                    {!sidebarCollapsed && (
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
                                    onClick={() => navigate(meta.path)}
                                    title={sidebarCollapsed ? meta.label : undefined}
                                    className={cx(
                                        "flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-[14px] font-medium transition-all",
                                        isActive
                                            ? "bg-primary-50 text-primary-700"
                                            : "text-text-muted hover:bg-slate-50 hover:text-text-main"
                                    )}
                                >
                                    <Icon className="h-5 w-5 shrink-0" />
                                    {!sidebarCollapsed && (
                                        <span className="truncate">{meta.label}</span>
                                    )}
                                </button>
                            );
                        })}
                    </div>
                </nav>

                {/* Collapse Button */}
                <div className="border-t border-border-main p-3">
                    <button
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

            {/* Main Content Area */}
            <div
                className={cx(
                    "flex flex-1 flex-col transition-all duration-300",
                    sidebarCollapsed ? "md:ml-[72px]" : "md:ml-[260px]"
                )}
            >
                {/* Topbar Utility */}
                <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center justify-between border-b border-border-main bg-white/80 px-4 backdrop-blur-md sm:px-6">
                    <div className="flex items-center gap-3">
                        <button className="rounded-xl p-2 text-text-muted transition hover:bg-slate-100 md:hidden">
                            <Menu className="h-5 w-5" />
                        </button>
                        <div>
                            <h1 className="text-[18px] font-semibold tracking-tight text-text-main">
                                {title}
                            </h1>
                        </div>
                    </div>

                    <div className="flex items-center gap-3">
                        <button className="relative rounded-full p-2 text-text-muted transition hover:bg-slate-100 hover:text-text-main">
                            <Bell className="h-5 w-5" />
                        </button>

                        <div className="h-6 w-px bg-border-main" />

                        <div className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-full bg-slate-900 text-sm font-semibold text-white transition hover:bg-slate-700">
                            A
                        </div>
                    </div>
                </header>

                <main className="flex-1 p-4 md:p-6 lg:p-8">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
