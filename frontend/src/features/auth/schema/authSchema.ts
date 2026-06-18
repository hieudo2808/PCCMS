import { z } from "zod";

export const loginSchema = z.object({
    email: z.string().min(1, "Email không được để trống").email("Email không hợp lệ"),
    password: z.string().min(1, "Mật khẩu không được để trống"),
});

export type LoginFormData = z.infer<typeof loginSchema>;

export const registerSchema = z
    .object({
        fullName: z.string().min(1, "Họ và tên không được để trống"),
        email: z.string().min(1, "Email không được để trống").email("Email không hợp lệ"),
        phone: z.string().min(1, "Số điện thoại không được để trống"),
        password: z.string().min(8, "Mật khẩu phải có ít nhất 8 ký tự"),
        confirmPassword: z.string().min(1, "Vui lòng xác nhận mật khẩu"),
    })
    .refine((data) => data.password === data.confirmPassword, {
        message: "Mật khẩu xác nhận không khớp",
        path: ["confirmPassword"],
    });

export type RegisterFormData = z.infer<typeof registerSchema>;
