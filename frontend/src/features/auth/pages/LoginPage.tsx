import { useNavigate } from "react-router-dom";
import { Button, Input } from "~/components/atoms";

export function LoginPage() {
    const navigate = useNavigate();

    return (
        <div className="space-y-6">
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-slate-900">Đăng nhập</h1>
                <p className="mt-2 text-sm text-slate-500">
                    Truy cập đúng dashboard theo vai trò của bạn
                </p>
            </div>
            <div className="space-y-4">
                <Input label="Email" placeholder="nhap@email.com" />
                <Input label="Mật khẩu" type="password" placeholder="••••••••" />
            </div>
            <div className="flex items-center justify-between text-sm">
                <label className="flex items-center gap-2 text-slate-600">
                    <input type="checkbox" className="rounded border-slate-300" />
                    Ghi nhớ đăng nhập
                </label>
                <button
                    onClick={() => navigate("/forgot-password")}
                    className="font-medium text-primary-700 transition hover:text-primary-800"
                >
                    Quên mật khẩu?
                </button>
            </div>
            <Button className="w-full py-3" variant="primary">
                Đăng nhập
            </Button>

            <p className="text-center text-sm text-slate-600">
                Chưa có tài khoản?{" "}
                <button
                    onClick={() => navigate("/register")}
                    className="font-medium text-primary-700 hover:text-primary-800"
                >
                    Đăng ký ngay
                </button>
            </p>
        </div>
    );
}
