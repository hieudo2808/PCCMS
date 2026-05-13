import { useRef, useState } from "react";
import { Mail, Phone, ShieldCheck, Trash2, Upload, User } from "lucide-react";
import { Button, Input } from "~/components/atoms";
import { Card } from "~/components/molecules";

type OtpStep = "idle" | "otp-sent";

const passwordHint = "Tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và chữ số.";

export function ProfilePage() {
    const avatarInputRef = useRef<HTMLInputElement>(null);

    const [profile, setProfile] = useState({
        name: "Nguyễn Minh Anh",
        email: "minhanh@email.com",
        phone: "0912 345 678",
        avatarFileName: "",
    });

    const [draftName, setDraftName] = useState(profile.name);
    const [draftAvatarFileName, setDraftAvatarFileName] = useState(profile.avatarFileName);

    const [emailStep, setEmailStep] = useState<OtpStep>("idle");
    const [newEmail, setNewEmail] = useState("");
    const [emailOtp, setEmailOtp] = useState("");

    const [phoneStep, setPhoneStep] = useState<OtpStep>("idle");
    const [newPhone, setNewPhone] = useState("");
    const [phoneOtp, setPhoneOtp] = useState("");

    const [passwordForm, setPasswordForm] = useState({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
    });

    const handleSaveProfile = (event: React.FormEvent) => {
        event.preventDefault();

        if (!draftName.trim()) {
            // TODO: thay bằng toast của hệ thống
            alert("Họ tên không được để trống.");
            return;
        }

        setProfile((current) => ({
            ...current,
            name: draftName.trim(),
            avatarFileName: draftAvatarFileName,
        }));

        // TODO: gọi API cập nhật profile: name + avatar
    };

    const handleChooseAvatar = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];

        if (!file) return;

        if (file.size > 20 * 1024 * 1024) {
            alert("Ảnh đại diện không được vượt quá 20MB.");
            return;
        }

        setDraftAvatarFileName(file.name);
    };

    const handleSendEmailOtp = (event: React.FormEvent) => {
        event.preventDefault();

        if (!newEmail.trim()) {
            alert("Vui lòng nhập email mới.");
            return;
        }

        setEmailStep("otp-sent");

        // TODO: gọi API gửi OTP về email mới
    };

    const handleConfirmEmail = (event: React.FormEvent) => {
        event.preventDefault();

        if (!emailOtp.trim()) {
            alert("Vui lòng nhập mã OTP.");
            return;
        }

        setProfile((current) => ({
            ...current,
            email: newEmail.trim(),
        }));

        setNewEmail("");
        setEmailOtp("");
        setEmailStep("idle");

        // TODO: gọi API xác nhận OTP và cập nhật email
    };

    const handleSendPhoneOtp = (event: React.FormEvent) => {
        event.preventDefault();

        if (!newPhone.trim()) {
            alert("Vui lòng nhập số điện thoại mới.");
            return;
        }

        setPhoneStep("otp-sent");

        // TODO: gọi API gửi OTP về số điện thoại mới
    };

    const handleConfirmPhone = (event: React.FormEvent) => {
        event.preventDefault();

        if (!phoneOtp.trim()) {
            alert("Vui lòng nhập mã OTP.");
            return;
        }

        setProfile((current) => ({
            ...current,
            phone: newPhone.trim(),
        }));

        setNewPhone("");
        setPhoneOtp("");
        setPhoneStep("idle");

        // TODO: gọi API xác nhận OTP và cập nhật số điện thoại
    };

    const handleChangePassword = (event: React.FormEvent) => {
        event.preventDefault();

        if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
            alert("Vui lòng nhập đầy đủ thông tin mật khẩu.");
            return;
        }

        if (passwordForm.newPassword !== passwordForm.confirmPassword) {
            alert("Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return;
        }

        // TODO: gọi API kiểm tra mật khẩu cũ và cập nhật mật khẩu mới

        setPasswordForm({
            currentPassword: "",
            newPassword: "",
            confirmPassword: "",
        });
    };

    return (
        <div className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
            <Card
                title="Hồ sơ người dùng"
                subtitle="Cập nhật ảnh đại diện và tên hiển thị"
            >
                <form onSubmit={handleSaveProfile} className="space-y-6">
                    <div className="flex flex-col items-center rounded-3xl bg-slate-50 p-6 text-center">
                        <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full bg-emerald-100 text-emerald-700">
                            <User className="h-10 w-10" />
                        </div>

                        <h3 className="mt-4 text-lg font-semibold">{draftName || profile.name}</h3>
                        <p className="text-sm text-slate-500">Chủ nuôi • ID: OW-1023</p>

                        {draftAvatarFileName ? (
                            <p className="mt-2 max-w-full truncate text-xs text-slate-500">
                                Ảnh đã chọn: {draftAvatarFileName}
                            </p>
                        ) : (
                            <p className="mt-2 text-xs text-slate-500">
                                Chưa có ảnh đại diện tùy chỉnh
                            </p>
                        )}

                        <input
                            ref={avatarInputRef}
                            type="file"
                            accept="image/*"
                            className="hidden"
                            onChange={handleChooseAvatar}
                        />

                        <div className="mt-4 flex gap-2">
                            <Button
                                type="button"
                                variant="outline"
                                onClick={() => avatarInputRef.current?.click()}
                            >
                                <Upload className="mr-2 h-4 w-4" />
                                Đổi ảnh
                            </Button>

                            <Button
                                type="button"
                                variant="ghost"
                                onClick={() => setDraftAvatarFileName("")}
                            >
                                <Trash2 className="mr-2 h-4 w-4" />
                                Xóa ảnh
                            </Button>
                        </div>

                        <p className="mt-3 text-xs text-slate-500">
                            Hỗ trợ ảnh dưới 20MB.
                        </p>
                    </div>

                    <Input
                        label="Họ tên"
                        value={draftName}
                        onChange={(event) => setDraftName(event.target.value)}
                    />

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
                        <Button type="submit">Lưu hồ sơ</Button>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => {
                                setDraftName(profile.name);
                                setDraftAvatarFileName(profile.avatarFileName);
                            }}
                        >
                            Hủy
                        </Button>
                    </div>
                </form>
            </Card>

            <div className="space-y-6">
                <Card
                    title="Cập nhật email"
                    subtitle="Mã OTP sẽ được gửi về email mới"
                >
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
                            <Input
                                label="Email mới"
                                type="email"
                                value={newEmail}
                                disabled
                            />

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
                            <Input
                                label="Số điện thoại mới"
                                type="tel"
                                value={newPhone}
                                disabled
                            />

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

                <Card
                    title="Thay đổi mật khẩu"
                    subtitle="Cập nhật mật khẩu đăng nhập tài khoản"
                >
                    <form onSubmit={handleChangePassword} className="space-y-4">
                        <div className="grid gap-4 md:grid-cols-3">
                            <Input
                                label="Mật khẩu cũ"
                                type="password"
                                value={passwordForm.currentPassword}
                                autoComplete="current-password"
                                onChange={(event) =>
                                    setPasswordForm((current) => ({
                                        ...current,
                                        currentPassword: event.target.value,
                                    }))
                                }
                            />

                            <Input
                                label="Mật khẩu mới"
                                type="password"
                                value={passwordForm.newPassword}
                                autoComplete="new-password"
                                onChange={(event) =>
                                    setPasswordForm((current) => ({
                                        ...current,
                                        newPassword: event.target.value,
                                    }))
                                }
                            />

                            <Input
                                label="Xác nhận mật khẩu mới"
                                type="password"
                                value={passwordForm.confirmPassword}
                                autoComplete="new-password"
                                onChange={(event) =>
                                    setPasswordForm((current) => ({
                                        ...current,
                                        confirmPassword: event.target.value,
                                    }))
                                }
                            />
                        </div>

                        <div className="flex items-start gap-2 rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
                            <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
                            <p>{passwordHint}</p>
                        </div>

                        <div className="flex justify-end">
                            <Button type="submit">Cập nhật mật khẩu</Button>
                        </div>
                    </form>
                </Card>
            </div>
        </div>
    );
}