import React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Modal } from "~/components/molecules/Modal";
import { Input } from "~/components/atoms/Input";
import { Button } from "~/components/atoms/Button";

const accountSchema = z.object({
    fullName: z.string().min(1, "Họ tên không được để trống"),
    email: z.string().email("Email không hợp lệ"),
    phone: z.string().min(1, "Số điện thoại không được để trống"),
    roleCode: z.string().min(1, "Vai trò không được để trống"),
    statusCode: z.enum(["UNVERIFIED", "ACTIVE", "LOCKED", "DISABLED"]).optional(),
});

export type AccountFormValues = z.infer<typeof accountSchema>;

interface AccountModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (data: AccountFormValues) => void;
    isSubmitting: boolean;
    mode?: "create" | "edit";
    initialValue?: Partial<AccountFormValues>;
}

const ROLE_OPTIONS = [
    { value: "", label: "Chọn vai trò" },
    { value: "ADMIN", label: "Admin" },
    { value: "VETERINARIAN", label: "Bác sĩ thú y" },
    { value: "STAFF", label: "Lễ tân" },
    { value: "OWNER", label: "Khách hàng" },
];

const STATUS_OPTIONS = [
    { value: "ACTIVE", label: "Hoạt động" },
    { value: "LOCKED", label: "Khóa" },
    { value: "DISABLED", label: "Vô hiệu hóa" },
    { value: "UNVERIFIED", label: "Chưa xác minh" },
];

export function AccountModal({
    isOpen,
    onClose,
    onSubmit,
    isSubmitting,
    mode = "create",
    initialValue,
}: AccountModalProps) {
    const {
        register,
        handleSubmit,
        formState: { errors },
        reset,
    } = useForm<AccountFormValues>({
        resolver: zodResolver(accountSchema),
        defaultValues: {
            fullName: "",
            email: "",
            phone: "",
            roleCode: "",
        },
    });

    React.useEffect(() => {
        if (isOpen) {
            reset({
                fullName: initialValue?.fullName ?? "",
                email: initialValue?.email ?? "",
                phone: initialValue?.phone ?? "",
                roleCode: initialValue?.roleCode ?? "",
                statusCode: initialValue?.statusCode ?? "ACTIVE",
            });
        }
    }, [initialValue, isOpen, reset]);

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={mode === "edit" ? "Sửa tài khoản" : "Thêm tài khoản mới"}>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-4">
                <Input
                    label="Họ tên"
                    id="fullName"
                    placeholder="Nhập họ tên"
                    {...register("fullName")}
                    error={errors.fullName?.message}
                />

                <Input
                    label="Email"
                    id="email"
                    type="email"
                    placeholder="Nhập email"
                    {...register("email")}
                    error={errors.email?.message}
                />

                <Input
                    label="Số điện thoại"
                    id="phone"
                    placeholder="Nhập số điện thoại"
                    {...register("phone")}
                    error={errors.phone?.message}
                />

                <div className="flex flex-col gap-1.5">
                    <label htmlFor="roleCode" className="text-[13px] font-medium text-slate-700">
                        Vai trò
                    </label>
                    <select
                        id="roleCode"
                        className={`h-10 w-full rounded-xl border bg-white px-3 text-[14px] text-slate-900 outline-none transition-all focus:ring-2 ${
                            errors.roleCode
                                ? "border-error-500 focus:border-error-500 focus:ring-error-500/20"
                                : "border-slate-200 hover:border-slate-300 focus:border-primary-500 focus:ring-primary-500/20"
                        }`}
                        {...register("roleCode")}
                    >
                        {ROLE_OPTIONS.map((opt) => (
                            <option key={opt.value} value={opt.value}>
                                {opt.label}
                            </option>
                        ))}
                    </select>
                    {errors.roleCode && <p className="text-[12px] font-medium text-error-600">{errors.roleCode.message}</p>}
                </div>

                {mode === "edit" && (
                    <div className="flex flex-col gap-1.5">
                        <label htmlFor="statusCode" className="text-[13px] font-medium text-slate-700">
                            Trạng thái
                        </label>
                        <select
                            id="statusCode"
                            className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-[14px] text-slate-900 outline-none transition-all hover:border-slate-300 focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                            {...register("statusCode")}
                        >
                            {STATUS_OPTIONS.map((opt) => (
                                <option key={opt.value} value={opt.value}>
                                    {opt.label}
                                </option>
                            ))}
                        </select>
                    </div>
                )}

                <div className="flex justify-end gap-3 pt-4">
                    <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>
                        Hủy
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Đang lưu..." : "Lưu"}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}
