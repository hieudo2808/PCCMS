import { useState, useEffect } from "react";
import { Mail, Phone, ShieldCheck, User } from "lucide-react";
import { Button, Input } from "~/components/atoms";
import { Card } from "~/components/molecules";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { userApi } from "~/shared/api/userApi";
import { SkeletonLoader } from "~/shared/components/SkeletonLoader";
import { ErrorState } from "~/shared/components/ErrorState";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import toast from "react-hot-toast";

type OtpStep = "idle" | "otp-sent";

const passwordHint = "Tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và chữ số.";

const profileSchema = z.object({
    fullName: z.string().min(1, "Họ tên không được để trống"),
});
type ProfileForm = z.infer<typeof profileSchema>;

const passwordSchema = z
    .object({
        oldPassword: z.string().min(1, "Vui lòng nhập đầy đủ thông tin mật khẩu."),
        newPassword: z.string().min(8, "Mật khẩu phải từ 8 ký tự"),
        confirmPassword: z.string().min(1, "Vui lòng nhập đầy đủ thông tin mật khẩu."),
    })
    .refine((data) => data.newPassword === data.confirmPassword, {
        message: "Mật khẩu mới và xác nhận mật khẩu không khớp.",
        path: ["confirmPassword"],
    });
type PasswordForm = z.infer<typeof passwordSchema>;

const roleLabels: Record<string, string> = {
    OWNER: "Chủ nuôi",
    STAFF: "Nhân viên trung tâm",
    VETERINARIAN: "Bác sĩ thú y",
    ADMIN: "Quản trị viên",
};

export function ProfilePage() {
    const queryClient = useQueryClient();

    const [emailStep, setEmailStep] = useState<OtpStep>("idle");
    const [newEmail, setNewEmail] = useState("");
    const [emailOtp, setEmailOtp] = useState("");

    const [phoneStep, setPhoneStep] = useState<OtpStep>("idle");
    const [newPhone, setNewPhone] = useState("");
    const [phoneOtp, setPhoneOtp] = useState("");

    // --- Data Fetching ---
    const {
        data: profile,
        isLoading,
        isError,
        refetch,
    } = useQuery({
        queryKey: ["profile"],
        queryFn: userApi.getProfile,
    });

    // --- Forms ---
    const {
        register: registerProfile,
        handleSubmit: handleProfileSubmit,
        reset: resetProfile,
        formState: { errors: profileErrors, isSubmitting: isUpdatingProfile },
    } = useForm<ProfileForm>({
        resolver: zodResolver(profileSchema),
    });

    const {
        register: registerPassword,
        handleSubmit: handlePasswordSubmit,
        reset: resetPassword,
        formState: { errors: passwordErrors, isSubmitting: isUpdatingPassword },
    } = useForm<PasswordForm>({
        resolver: zodResolver(passwordSchema),
    });

    // Load data into forms when fetched
    useEffect(() => {
        if (profile) {
            resetProfile({
                fullName: profile.fullName,
            });
        }
    }, [profile, resetProfile]);

    // --- Mutations ---
    const updateProfileMutation = useMutation({
        mutationFn: userApi.updateProfile,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["profile"] });
            toast.success("Cập nhật hồ sơ thành công");
        },
        onError: () => {
            toast.error("Không thể cập nhật hồ sơ");
        },
    });

    const changePasswordMutation = useMutation({
        mutationFn: userApi.changePassword,
        onSuccess: () => {
            resetPassword();
            toast.success("Cập nhật mật khẩu thành công");
        },
        onError: () => {
            toast.error("Không thể cập nhật mật khẩu");
        },
    });

    // --- Handlers ---
    const onSaveProfile = (data: ProfileForm) => {
        updateProfileMutation.mutate({
            fullName: data.fullName,
        });
    };

    const onChangePassword = (data: PasswordForm) => {
        changePasswordMutation.mutate({
            currentPassword: data.oldPassword,
            newPassword: data.newPassword,
        });
    };

    const requestEmailOtpMutation = useMutation({
        mutationFn: userApi.requestEmailChangeOtp,
        onSuccess: () => {
            setEmailStep("otp-sent");
            toast.success("Đã gửi OTP");
        },
        onError: () => toast.error("Không thể gửi OTP"),
    });

    const confirmEmailMutation = useMutation({
        mutationFn: userApi.confirmEmailChange,
        onSuccess: () => {
            setNewEmail("");
            setEmailOtp("");
            setEmailStep("idle");
            queryClient.invalidateQueries({ queryKey: ["profile"] });
            toast.success("Đổi email thành công!");
        },
        onError: () => toast.error("Không thể đổi email"),
    });

    const requestPhoneOtpMutation = useMutation({
        mutationFn: userApi.requestPhoneChangeOtp,
        onSuccess: () => {
            setPhoneStep("otp-sent");
            toast.success("Đã gửi OTP");
        },
        onError: () => toast.error("Không thể gửi OTP"),
    });

    const confirmPhoneMutation = useMutation({
        mutationFn: userApi.confirmPhoneChange,
        onSuccess: () => {
            setNewPhone("");
            setPhoneOtp("");
            setPhoneStep("idle");
            queryClient.invalidateQueries({ queryKey: ["profile"] });
            toast.success("Đổi số điện thoại thành công!");
        },
        onError: () => toast.error("Không thể đổi số điện thoại"),
    });

    const handleSendEmailOtp = (event: React.FormEvent) => {
        event.preventDefault();
        if (!newEmail.trim()) return toast.error("Vui lòng nhập email mới.");
        requestEmailOtpMutation.mutate({ contact: newEmail.trim() });
    };
    const handleConfirmEmail = (event: React.FormEvent) => {
        event.preventDefault();
        if (!emailOtp.trim()) return toast.error("Vui lòng nhập mã OTP.");
        confirmEmailMutation.mutate({ contact: newEmail.trim(), otp: emailOtp.trim() });
    };
    const handleSendPhoneOtp = (event: React.FormEvent) => {
        event.preventDefault();
        if (!newPhone.trim()) return toast.error("Vui lòng nhập số điện thoại mới.");
        requestPhoneOtpMutation.mutate({ contact: newPhone.trim() });
    };
    const handleConfirmPhone = (event: React.FormEvent) => {
        event.preventDefault();
        if (!phoneOtp.trim()) return toast.error("Vui lòng nhập mã OTP.");
        confirmPhoneMutation.mutate({ contact: newPhone.trim(), otp: phoneOtp.trim() });
    };

    // --- Render States ---
    if (isLoading) {
        return (
            <div data-testid="profile-skeleton" className="p-6">
                <SkeletonLoader className="h-[400px] w-full rounded-2xl" />
            </div>
        );
    }

    if (isError || !profile) {
        return (
            <ErrorState onRetry={() => refetch()} message="Không thể tải thông tin người dùng" />
        );
    }

    return (
        <div className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
            <Card title="Hồ sơ người dùng" subtitle="Cập nhật tên hiển thị">
                <form onSubmit={handleProfileSubmit(onSaveProfile)} className="space-y-6">
                    <div className="flex flex-col items-center rounded-3xl bg-slate-50 p-6 text-center">
                        <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full bg-emerald-100 text-emerald-700">
                            <User className="h-10 w-10" />
                        </div>

                        <h3 className="mt-4 text-lg font-semibold">{profile.fullName}</h3>
                        <p className="text-sm text-slate-500">
                            {roleLabels[profile.roleCode] || profile.roleCode} • ID:{" "}
                            {profile.id.substring(0, 8)}
                        </p>
                    </div>

                    <div>
                        <Input
                            label="Họ tên"
                            {...registerProfile("fullName")}
                            error={profileErrors.fullName?.message}
                        />
                    </div>

                    <div className="rounded-2xl border border-slate-200 p-4">
                        <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                            Thông tin liên hệ hiện tại
                        </p>
                        <div className="mt-3 space-y-2 text-sm">
                            <div className="flex items-center gap-2 text-slate-600">
                                <Mail className="h-4 w-4" />
                                <span>{profile.email}</span>
                            </div>
                            <div className="flex items-center gap-2 text-slate-600">
                                <Phone className="h-4 w-4" />
                                <span>{profile.phone}</span>
                            </div>
                        </div>
                        <p className="mt-3 text-xs text-slate-500">
                            Email và số điện thoại được đổi ở panel riêng bằng mã OTP.
                        </p>
                    </div>

                    <div className="flex gap-2">
                        <Button type="submit" disabled={isUpdatingProfile}>
                            {isUpdatingProfile ? "Đang lưu..." : "Lưu hồ sơ"}
                        </Button>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => {
                                resetProfile({ fullName: profile.fullName });
                            }}
                            disabled={isUpdatingProfile}
                        >
                            Hủy
                        </Button>
                    </div>
                </form>
            </Card>

            <div className="space-y-6">
                <Card title="Cập nhật email" subtitle="Mã OTP sẽ được gửi về email mới">
                    <div className="mb-4 rounded-2xl bg-slate-50 p-4">
                        <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                            Email hiện tại
                        </p>
                        <p className="mt-1 text-sm font-medium text-slate-700">{profile.email}</p>
                    </div>
                    {emailStep === "idle" ? (
                        <form onSubmit={handleSendEmailOtp} className="space-y-4">
                            <Input
                                label="Email mới"
                                type="email"
                                placeholder="Email mới"
                                value={newEmail}
                                onChange={(event) => setNewEmail(event.target.value)}
                            />
                            <div className="flex justify-end">
                                <Button type="submit" variant="outline">
                                    Gửi OTP
                                </Button>
                            </div>
                        </form>
                    ) : (
                        <form onSubmit={handleConfirmEmail} className="space-y-4">
                            <Input label="Email mới" type="email" value={newEmail} disabled />

                            <Input
                                label="Mã OTP"
                                placeholder="6 chữ số"
                                value={emailOtp}
                                inputMode="numeric"
                                maxLength={6}
                                onChange={(event) => setEmailOtp(event.target.value)}
                            />
                            <div className="flex flex-wrap gap-2">
                                <Button type="submit">Xác nhận đổi email</Button>
                                <Button
                                    type="button"
                                    variant="outline"
                                    onClick={() => {
                                        setEmailStep("idle");
                                        setEmailOtp("");
                                    }}
                                >
                                    Đổi email khác
                                </Button>
                            </div>
                        </form>
                    )}
                </Card>

                <Card
                    title="Cập nhật số điện thoại"
                    subtitle="Mã OTP sẽ được gửi về số điện thoại mới"
                >
                    <div className="mb-4 rounded-2xl bg-slate-50 p-4">
                        <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                            Số điện thoại hiện tại
                        </p>
                        <p className="mt-1 text-sm font-medium text-slate-700">{profile.phone}</p>
                    </div>
                    {phoneStep === "idle" ? (
                        <form onSubmit={handleSendPhoneOtp} className="space-y-4">
                            <Input
                                label="Số điện thoại mới"
                                type="tel"
                                placeholder="Số điện thoại mới"
                                value={newPhone}
                                onChange={(event) => setNewPhone(event.target.value)}
                            />
                            <div className="flex justify-end">
                                <Button type="submit" variant="outline">
                                    Gửi OTP
                                </Button>
                            </div>
                        </form>
                    ) : (
                        <form onSubmit={handleConfirmPhone} className="space-y-4">
                            <Input label="Số điện thoại mới" type="tel" value={newPhone} disabled />

                            <Input
                                label="Mã OTP"
                                placeholder="6 chữ số"
                                value={phoneOtp}
                                inputMode="numeric"
                                maxLength={6}
                                onChange={(event) => setPhoneOtp(event.target.value)}
                            />
                            <div className="flex flex-wrap gap-2">
                                <Button type="submit">Xác nhận đổi số điện thoại</Button>
                                <Button
                                    type="button"
                                    variant="outline"
                                    onClick={() => {
                                        setPhoneStep("idle");
                                        setPhoneOtp("");
                                    }}
                                >
                                    Đổi số khác
                                </Button>
                            </div>
                        </form>
                    )}
                </Card>

                <Card title="Thay đổi mật khẩu" subtitle="Cập nhật mật khẩu đăng nhập tài khoản">
                    <form onSubmit={handlePasswordSubmit(onChangePassword)} className="space-y-4">
                        <div className="grid gap-4 md:grid-cols-3">
                            <Input
                                label="Mật khẩu cũ"
                                type="password"
                                autoComplete="current-password"
                                {...registerPassword("oldPassword")}
                                error={passwordErrors.oldPassword?.message}
                            />
                            <Input
                                label="Mật khẩu mới"
                                type="password"
                                autoComplete="new-password"
                                {...registerPassword("newPassword")}
                                error={passwordErrors.newPassword?.message}
                            />
                            <Input
                                label="Xác nhận mật khẩu mới"
                                type="password"
                                autoComplete="new-password"
                                {...registerPassword("confirmPassword")}
                                error={passwordErrors.confirmPassword?.message}
                            />
                        </div>
                        <div className="flex items-start gap-2 rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
                            <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
                            <p>{passwordHint}</p>
                        </div>
                        <div className="flex justify-end">
                            <Button type="submit" disabled={isUpdatingPassword}>
                                {isUpdatingPassword ? "Đang lưu..." : "Cập nhật mật khẩu"}
                            </Button>
                        </div>
                    </form>
                </Card>
            </div>
        </div>
    );
}
