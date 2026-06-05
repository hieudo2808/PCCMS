import { z } from 'zod';

export const PET_AVATAR_MAX_BYTES = 2 * 1024 * 1024; // 2MB

export const PET_MESSAGES = {
  requiredFields: 'Vui lòng nhập đầy đủ các thông tin bắt buộc',
  requiredFieldsRegister: 'Vui lòng nhập đầy đủ các trường thông tin bắt buộc',
  invalidWeight: 'Cân nặng phải là một con số hợp lệ',
  imageTooLarge: 'Kích thước ảnh quá lớn',
  createSuccess: 'Thêm thú cưng thành công',
  registerSuccess: 'Đăng ký hồ sơ thú cưng thành công',
  updateSuccess: 'Chỉnh sửa hồ sơ thành công',
  deleteSuccess: 'Xoá hồ sơ thành công',
  emptyList: 'Bạn chưa có thú cưng nào',
  deleteConfirm: (name: string) =>
    `Bạn có chắc chắn muốn xóa hồ sơ của bé ${name} không?`,
  noBoardingPets: 'Bạn hiện không có thú cưng nào đang lưu trú tại trung tâm',
  careLogsUpdating: 'Nhật ký chăm sóc đang được cập nhật',
} as const;

const nameSchema = z
  .string()
  .min(1, PET_MESSAGES.requiredFields)
  .max(50, 'Tên thú cưng tối đa 50 ký tự')
  .regex(/^[\p{L}0-9 ]+$/u, 'Tên không chứa ký tự đặc biệt (trừ dấu cách)');

const weightSchema = z
  .string()
  .min(1, PET_MESSAGES.requiredFields)
  .refine((v) => /^\d+(\.\d{1,2})?$/.test(v), PET_MESSAGES.invalidWeight)
  .refine((v) => parseFloat(v) > 0, PET_MESSAGES.invalidWeight);

const optionalWeightSchema = z
  .string()
  .optional()
  .refine((v) => !v || /^\d+(\.\d{1,2})?$/.test(v), PET_MESSAGES.invalidWeight)
  .refine((v) => !v || parseFloat(v) > 0, PET_MESSAGES.invalidWeight);

const ageMonthsSchema = z
  .string()
  .optional()
  .refine((v) => !v || (/^\d+$/.test(v) && Number(v) >= 0), 'Tuổi ước tính phải là số nguyên không âm');

export const petFormSchema = z
  .object({
    name: nameSchema,
    speciesId: z.string().min(1, PET_MESSAGES.requiredFields),
    breedId: z.string().optional(),
    sex: z.enum(['MALE', 'FEMALE'], { message: PET_MESSAGES.requiredFields }),
    birthDate: z.string().optional(),
    estimatedAgeMonths: ageMonthsSchema,
    weightKg: weightSchema,
    color: z.string().max(50, 'Màu lông tối đa 50 ký tự').optional(),
    identificationNote: z.string().optional(),
    specialNote: z.string().max(500, 'Ghi chú đặc biệt tối đa 500 ký tự').optional(),
    allergyNote: z.string().optional(),
    nutritionNote: z.string().optional(),
  })
  .refine(
    (data) => Boolean(data.birthDate?.trim()) || Boolean(data.estimatedAgeMonths?.trim()),
    { message: 'Nhập ngày sinh hoặc tuổi ước tính', path: ['birthDate'] }
  )
  .refine(
    (data) => {
      if (!data.birthDate?.trim()) return true;
      const d = new Date(data.birthDate);
      const today = new Date();
      today.setHours(23, 59, 59, 999);
      return !Number.isNaN(d.getTime()) && d <= today;
    },
    { message: 'Ngày sinh phải hợp lệ và không lớn hơn ngày hiện tại', path: ['birthDate'] }
  );

export type PetFormValues = z.infer<typeof petFormSchema>;
