import { useState } from "react";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { Button, Input } from "~/components/atoms";
import { authApi } from "~/features/auth/api/authApi";

export function ForgotPasswordPage() {
    const navigate = useNavigate();
    const [step, setStep] = useState<1 | 2>(1);
    const [contact, setContact] = useState("");
    const [otp, setOtp] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    const requestOtp = async () => {
        if (!contact.trim()) {
            toast.error("Nhập email hoặc số điện thoại");
            return;
        }
        setIsSubmitting(true);
        try {
            await authApi.requestPasswordResetOtp({ contact: contact.trim() });
            setStep(2);
            toast.success("Đã gửi mã xác thực");
        } catch {
            toast.error("Không thể gửi mã xác thực");
        } finally {
            setIsSubmitting(false);
        }
    };

    const confirmReset = async () => {
        if (!otp.trim() || !newPassword || !confirmPassword) {
            toast.error("Nhập đầy đủ thông tin");
            return;
        }
        if (newPassword !== confirmPassword) {
            toast.error("Mật khẩu xác nhận không khớp");
            return;
        }
        setIsSubmitting(true);
        try {
            await authApi.confirmPasswordReset({
                contact: contact.trim(),
                otp: otp.trim(),
                newPassword,
            });
            toast.success("Đã đặt lại mật khẩu");
            navigate("/login");
        } catch {
            toast.error("Không thể đặt lại mật khẩu");
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="space-y-6">
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-slate-900">Khôi phục mật khẩu</h1>
                <p className="mt-2 text-sm text-slate-500">
                    {step === 1
                        ? "Nhập email hoặc số điện thoại để nhận mã OTP."
                        : "Nhập OTP và mật khẩu mới cho tài khoản của bạn."}
                </p>
            </div>

            <div className="min-h-[160px]">
                {step === 1 && (
                    <div className="space-y-4 animate-in fade-in slide-in-from-bottom-2">
                        <Input
                            label="Email hoặc số điện thoại"
                            placeholder="name@domain.com"
                            value={contact}
                            onChange={(event) => setContact(event.target.value)}
                        />
                        <Button
                            className="w-full py-3"
                            variant="primary"
                            disabled={isSubmitting}
                            onClick={requestOtp}
                        >
                            {isSubmitting ? "Đang gửi..." : "Gửi mã xác thực"}
                        </Button>
                    </div>
                )}

                {step === 2 && (
                    <div className="space-y-4 animate-in fade-in slide-in-from-bottom-2">
                        <Input
                            label="Mã xác thực OTP"
                            placeholder="Nhập 6 chữ số"
                            maxLength={6}
                            value={otp}
                            onChange={(event) => setOtp(event.target.value)}
                        />
                        <Input
                            label="Mật khẩu mới"
                            type="password"
                            value={newPassword}
                            onChange={(event) => setNewPassword(event.target.value)}
                        />
                        <Input
                            label="Xác nhận mật khẩu"
                            type="password"
                            value={confirmPassword}
                            onChange={(event) => setConfirmPassword(event.target.value)}
                        />
                        <Button
                            className="w-full py-3"
                            variant="primary"
                            disabled={isSubmitting}
                            onClick={confirmReset}
                        >
                            {isSubmitting ? "Đang lưu..." : "Lưu mật khẩu và đăng nhập"}
                        </Button>
                        <div className="pt-2 text-center">
                            <button
                                type="button"
                                className="text-sm font-medium text-slate-500 transition hover:text-primary-700"
                                disabled={isSubmitting}
                                onClick={requestOtp}
                            >
                                Chưa nhận được mã? Gửi lại
                            </button>
                        </div>
                    </div>
                )}
            </div>

            <p className="border-t border-slate-100 pt-5 text-center text-sm text-slate-600">
                <button
                    type="button"
                    onClick={() => navigate("/login")}
                    className="font-medium text-primary-700 hover:text-primary-800"
                >
                    &larr; Quay lại trang đăng nhập
                </button>
            </p>
        </div>
    );
}
