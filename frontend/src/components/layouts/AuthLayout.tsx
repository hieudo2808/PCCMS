import { Outlet } from "react-router-dom";
import { PawPrint } from "lucide-react";

export function AuthLayout() {
    return (
        <div className="flex min-h-screen">
            <div className="relative hidden w-[45%] flex-col bg-slate-900 lg:flex">
                <div className="absolute inset-0 bg-linear-to-b from-primary-700 to-slate-900 opacity-90" />
                <div className="relative z-10 flex h-full flex-col justify-between p-12 text-white">
                    <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/20">
                            <PawPrint className="h-6 w-6 text-white" />
                        </div>
                        <span className="text-xl font-bold tracking-tight">Pawluna</span>
                    </div>
                    <div className="max-w-md">
                        <h2 className="text-3xl font-bold tracking-tight">
                            Trung tâm Quản lý Chăm sóc Thú cưng.
                        </h2>
                        <p className="mt-4 text-pretty font-medium text-slate-300">
                            Dễ dàng đặt lịch khám, spa và lưu trú. Hồ sơ bệnh án thông minh và nhắc
                            nhở tiêm phòng tự động.
                        </p>
                    </div>
                    <div className="text-sm font-medium text-slate-400">© 2026 Astral Team.</div>
                </div>
            </div>

            <div className="flex flex-1 items-center justify-center bg-white px-4 py-12 sm:px-6 lg:px-8">
                <div className="w-full max-w-sm">
                    <div className="mb-8 flex items-center justify-center gap-3 lg:hidden">
                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-600">
                            <PawPrint className="h-6 w-6 text-white" />
                        </div>
                        <span className="text-xl font-bold tracking-tight text-slate-900">
                            Pawluna
                        </span>
                    </div>

                    <Outlet />
                </div>
            </div>
        </div>
    );
}
