import { useState } from "react";
import type { RoleKey } from "~/types/navigation";
import { mockAuth, setDevRole } from "~/constants/auth";

// ─── Role metadata ────────────────────────────────────────────────────────────
const ROLES: { key: RoleKey; label: string; emoji: string; home: string; color: string }[] = [
    { key: "owner",     label: "Chủ nuôi",  emoji: "🐾", home: "/owner",            color: "bg-violet-500" },
    { key: "reception", label: "Lễ tân",    emoji: "🗓️", home: "/reception",        color: "bg-sky-500"    },
    { key: "doctor",    label: "Bác sĩ",    emoji: "🩺", home: "/doctor",           color: "bg-teal-500"   },
    { key: "admin",     label: "Quản trị",  emoji: "⚙️", home: "/admin",            color: "bg-rose-500"   },
];

// ─── Component ────────────────────────────────────────────────────────────────
export function DevRoleSwitcher() {
    const [open, setOpen] = useState(false);
    const [current, setCurrent] = useState<RoleKey>(mockAuth.currentRole);

    // Only show in development
    if (import.meta.env.PROD) return null;

    const currentMeta = ROLES.find((r) => r.key === current) ?? ROLES[0];

    const switchTo = (role: (typeof ROLES)[0]) => {
        setDevRole(role.key);
        setCurrent(role.key);
        setOpen(false);
        window.location.href = role.home;
    };

    return (
        <div className="fixed bottom-5 right-5 z-[9999] flex flex-col items-end gap-2">
            {/* Role list */}
            {open && (
                <div className="flex flex-col gap-1.5 rounded-2xl border border-slate-200 bg-white p-3 shadow-xl animate-in slide-in-from-bottom-2">
                    <p className="mb-1 px-1 text-[11px] font-semibold uppercase tracking-widest text-slate-400">
                        Chuyển role
                    </p>
                    {ROLES.map((role) => (
                        <button
                            key={role.key}
                            onClick={() => switchTo(role)}
                            className={[
                                "flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-all",
                                current === role.key
                                    ? "bg-slate-100 text-slate-800 ring-1 ring-slate-300"
                                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-800",
                            ].join(" ")}
                        >
                            <span
                                className={`flex h-7 w-7 items-center justify-center rounded-full text-white text-xs ${role.color}`}
                            >
                                {role.emoji}
                            </span>
                            <span>{role.label}</span>
                            {current === role.key && (
                                <span className="ml-auto text-[10px] rounded-full bg-slate-200 px-1.5 py-0.5 text-slate-500">
                                    Hiện tại
                                </span>
                            )}
                        </button>
                    ))}
                    <div className="mt-1 border-t border-slate-100 pt-2 px-1">
                        <p className="text-[10px] text-slate-400">
                            🛠 Chỉ hiển thị trong môi trường <strong>dev</strong>
                        </p>
                    </div>
                </div>
            )}

            {/* Toggle button */}
            <button
                onClick={() => setOpen((o) => !o)}
                className={`flex items-center gap-2 rounded-2xl px-4 py-2.5 text-sm font-semibold text-white shadow-lg transition-all hover:opacity-90 active:scale-95 ${currentMeta.color}`}
                title="Dev: Chuyển đổi role"
            >
                <span>{currentMeta.emoji}</span>
                <span>{currentMeta.label}</span>
                <span className="ml-1 text-white/70">{open ? "▲" : "▼"}</span>
            </button>
        </div>
    );
}
