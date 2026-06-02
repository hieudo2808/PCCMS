import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Input } from "~/components/atoms";

export function ForgotPasswordPage() {
    const navigate = useNavigate();
    const [step, setStep] = useState<1 | 2 | 3>(1);

    return (
        <div className="space-y-6">
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-slate-900">Khôi phục mật khẩu</h1>
                <p className="mt-2 text-sm text-slate-500">
                    {step === 1 && "Nhập thông tin tài khoản để nhận hướng dẫn khôi phục bảo mật."}
                    {step === 2 && "Vui lòng nhập mã bảo mật 6 số vừa được gửi tới bạn."}
                    {step === 3 && "Thiết lập lại mật khẩu mới cho tài khoản của bạn."}
                </p>
            </div>

            <div className="min-h-[160px]">
                {step === 1 && (
                    <div className="space-y-4 animate-in fade-in slide-in-from-bottom-2">
                        <Input
                            label="Email hoặc Số điện thoại"
                            placeholder="name@domain.com hoặc 0912..."
                        />
                        <Button
                            className="w-full py-3"
                            variant="primary"
                            onClick={() => setStep(2)}
                        >
                            Gửi mã xác thực
                        </Button>
                    </div>
                )}

                {step === 2 && (
                    <div className="space-y-4 animate-in fade-in slide-in-from-bottom-2">
                        <Input
                            label="Mã xác thực (OTP)"
                            placeholder="Nhập 6 chữ số"
                            maxLength={6}
                        />
                        <Button
                            className="w-full py-3"
                            variant="primary"
                            onClick={() => setStep(3)}
                        >
                            Xác minh mã
                        </Button>
                        <div className="text-center pt-2">
                            <button className="text-sm font-medium text-slate-500 transition hover:text-primary-700">
                                Chưa nhận được mã? Gửi lại
                            </button>
                        </div>
                    </div>
                )}

                {step === 3 && (
                    <div className="space-y-4 animate-in fade-in slide-in-from-bottom-2">
                        <Input label="Mật khẩu mới" type="password" placeholder="••••••••" />
                        <Input label="Xác nhận mật khẩu" type="password" placeholder="••••••••" />
                        <Button
                            className="w-full py-3"
                            variant="primary"
                            onClick={() => navigate("/login")}
                        >
                            Lưu mật khẩu & Đăng nhập
                        </Button>
                    </div>
                )}
            </div>

            <p className="text-center text-sm text-slate-600 border-t border-slate-100 pt-5">
                <button
                    onClick={() => navigate("/login")}
                    className="font-medium text-primary-700 hover:text-primary-800"
                >
                    &larr; Quay lại trang đăng nhập
                </button>
            </p>
        </div>
    );
}
