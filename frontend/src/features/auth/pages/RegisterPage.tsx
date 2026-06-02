import { useNavigate } from "react-router-dom";
import { Button, Input } from "~/components/atoms";

export function RegisterPage() {
    const navigate = useNavigate();

    return (
        <div className="space-y-6">
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-slate-900">Tạo tài khoản chủ nuôi</h1>
                <p className="mt-2 text-sm text-slate-500">
                    Bắt đầu quản lý hồ sơ thú cưng và đặt dịch vụ trực tuyến
                </p>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
                <Input label="Họ tên" placeholder="Nguyễn Văn A" />
                <Input label="Số điện thoại" placeholder="0912 345 678" />
            </div>
            <Input label="Email" placeholder="owner@email.com" />
            <div className="grid gap-4 md:grid-cols-2">
                <Input label="Mật khẩu" type="password" placeholder="••••••••" />
                <Input label="Xác nhận mật khẩu" type="password" placeholder="••••••••" />
            </div>

            <Button className="w-full py-3" variant="primary">
                Tạo tài khoản
            </Button>

            <p className="text-center text-sm text-slate-600">
                Đã có tài khoản?{" "}
                <button
                    onClick={() => navigate("/login")}
                    className="font-medium text-primary-700 hover:text-primary-800"
                >
                    Về trang đăng nhập
                </button>
            </p>
        </div>
    );
}
