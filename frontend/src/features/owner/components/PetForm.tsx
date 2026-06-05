import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Button, Input, Select, Textarea } from '~/components/atoms';
import { petApi } from '~/shared/api/petApi';
import { petCatalogApi } from '~/shared/api/petCatalogApi';
import type { PetRequest, PetResponse } from '~/types/pet';
import { hasAccessToken } from '~/shared/auth/tokenStorage';
import {
  petFormSchema,
  PET_MESSAGES,
  type PetFormValues,
} from '../schema/petSchema';
import { PetAvatarUpload } from './PetAvatarUpload';

interface PetFormProps {
  petId?: string;
  onSuccess: () => void;
  onCancel: () => void;
}

function toFormValues(pet?: PetResponse): PetFormValues {
  return {
    name: pet?.name ?? '',
    speciesId: pet?.speciesId ?? '',
    breedId: pet?.breedId ?? '',
    sex: pet?.sex === 'FEMALE' ? 'FEMALE' : pet?.sex === 'MALE' ? 'MALE' : 'MALE',
    birthDate: pet?.birthDate ?? '',
    estimatedAgeMonths:
      pet?.estimatedAgeMonths != null ? String(pet.estimatedAgeMonths) : '',
    weightKg: pet?.weightKg != null ? String(pet.weightKg) : '',
    color: pet?.color ?? '',
    identificationNote: pet?.identificationNote ?? '',
    specialNote: pet?.specialNote ?? '',
    allergyNote: pet?.allergyNote ?? '',
    nutritionNote: pet?.nutritionNote ?? '',
  };
}

function toRequest(data: PetFormValues): PetRequest {
  return {
    name: data.name.trim(),
    speciesId: data.speciesId,
    breedId: data.breedId || undefined,
    sex: data.sex,
    birthDate: data.birthDate || undefined,
    estimatedAgeMonths: data.estimatedAgeMonths
      ? Number(data.estimatedAgeMonths)
      : undefined,
    weightKg: Number(data.weightKg),
    color: data.color || undefined,
    identificationNote: data.identificationNote || undefined,
    specialNote: data.specialNote || undefined,
    allergyNote: data.allergyNote || undefined,
    nutritionNote: data.nutritionNote || undefined,
  };
}

export function PetForm({ petId, onSuccess, onCancel }: PetFormProps) {
  const queryClient = useQueryClient();
  const isEdit = Boolean(petId);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [avatarError, setAvatarError] = useState<string | null>(null);

  const { data: petDetail, isLoading: petLoading } = useQuery({
    queryKey: ['pet', petId],
    queryFn: () => petApi.getPetById(petId as string),
    enabled: isEdit && hasAccessToken(),
  });

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<PetFormValues>({
    resolver: zodResolver(petFormSchema),
    defaultValues: toFormValues(),
  });

  useEffect(() => {
    if (petDetail) {
      reset(toFormValues(petDetail));
    }
  }, [petDetail, reset]);

  const selectedSpeciesId = watch('speciesId');
  const catalogEnabled = hasAccessToken();

  const {
    data: speciesOptions = [],
    isLoading: speciesLoading,
    isError: speciesError,
  } = useQuery({
    queryKey: ['pet-catalog', 'species'],
    queryFn: () => petCatalogApi.getSpecies(),
    enabled: catalogEnabled,
    staleTime: 5 * 60 * 1000,
  });

  const {
    data: breedOptions = [],
    isLoading: breedsLoading,
    isError: breedsError,
  } = useQuery({
    queryKey: ['pet-catalog', 'breeds', selectedSpeciesId],
    queryFn: () => petCatalogApi.getBreedsBySpecies(selectedSpeciesId),
    enabled: catalogEnabled && Boolean(selectedSpeciesId),
    staleTime: 5 * 60 * 1000,
  });

  const saveMutation = useMutation({
    mutationFn: (data: PetRequest) =>
      isEdit && petId
        ? petApi.updatePet(petId, data)
        : petApi.createPet(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pets'] });
      if (petId) {
        queryClient.invalidateQueries({ queryKey: ['pet', petId] });
      }
      toast.success(
        isEdit ? PET_MESSAGES.updateSuccess : PET_MESSAGES.registerSuccess
      );
      onSuccess();
    },
    onError: () => {
      toast.error('Không thể lưu hồ sơ. Vui lòng thử lại.');
    },
  });

  const onSubmit = (data: PetFormValues) => {
    if (avatarError) {
      toast.error(avatarError);
      return;
    }
    saveMutation.mutate(toRequest(data));
  };

  const onInvalid = () => {
    toast.error(PET_MESSAGES.requiredFields);
  };

  const speciesSelectOptions = speciesOptions.map((s) => ({
    value: s.id,
    label: s.name,
  }));

  const breedSelectOptions = breedOptions.map((b) => ({
    value: b.id,
    label: b.name,
  }));

  if (isEdit && petLoading) {
    return (
      <div className="flex justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-indigo-600" />
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit, onInvalid)} className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <Input
          label="Tên thú cưng"
          required
          placeholder="Milu"
          maxLength={50}
          {...register('name')}
          error={errors.name?.message}
        />
        <Controller
          name="speciesId"
          control={control}
          render={({ field }) => (
            <Select
              label="Loài"
              required
              placeholder={
                speciesLoading
                  ? 'Đang tải loài...'
                  : speciesSelectOptions.length === 0
                    ? 'Chưa có dữ liệu loài'
                    : 'Chọn loài'
              }
              options={speciesSelectOptions}
              value={field.value ?? ''}
              onChange={(e) => {
                field.onChange(e.target.value);
                setValue('breedId', '');
              }}
              onBlur={field.onBlur}
              name={field.name}
              ref={field.ref}
              error={
                errors.speciesId?.message
                ?? (speciesError ? 'Không tải được danh sách loài' : undefined)
              }
              disabled={speciesLoading || speciesSelectOptions.length === 0}
            />
          )}
        />
        <Controller
          name="breedId"
          control={control}
          render={({ field }) => (
            <Select
              label="Giống"
              placeholder={
                !selectedSpeciesId
                  ? 'Chọn loài trước'
                  : breedsLoading
                    ? 'Đang tải giống...'
                    : breedSelectOptions.length === 0
                      ? 'Không có giống (tuỳ chọn)'
                      : 'Chọn giống (tuỳ chọn)'
              }
              options={breedSelectOptions}
              value={field.value ?? ''}
              onChange={(e) => field.onChange(e.target.value)}
              onBlur={field.onBlur}
              name={field.name}
              ref={field.ref}
              error={breedsError ? 'Không tải được danh sách giống' : undefined}
              disabled={!selectedSpeciesId || breedsLoading}
            />
          )}
        />
        <Controller
          name="sex"
          control={control}
          render={({ field }) => (
            <div className="flex flex-col gap-1.5">
              <span className="text-[13px] font-medium text-slate-700">
                Giới tính <span className="text-error-500">*</span>
              </span>
              <div className="flex gap-4 pt-2">
                {([
                  ['MALE', 'Đực'],
                  ['FEMALE', 'Cái'],
                ] as const).map(([value, label]) => (
                  <label key={value} className="inline-flex cursor-pointer items-center gap-2 text-sm">
                    <input
                      type="radio"
                      name={field.name}
                      value={value}
                      checked={field.value === value}
                      onChange={() => field.onChange(value)}
                      onBlur={field.onBlur}
                      className="h-4 w-4 text-emerald-600"
                    />
                    {label}
                  </label>
                ))}
              </div>
              {errors.sex?.message && (
                <p className="text-[12px] font-medium text-error-600">{errors.sex.message}</p>
              )}
            </div>
          )}
        />
        <Input
          label="Cân nặng (kg)"
          required
          placeholder="4.5"
          type="number"
          step="0.01"
          min={0.01}
          {...register('weightKg')}
          error={errors.weightKg?.message}
        />
        <Input
          label="Ngày sinh"
          type="date"
          max={new Date().toISOString().split('T')[0]}
          {...register('birthDate')}
          error={errors.birthDate?.message}
        />
        <Input
          label="Tuổi ước tính (tháng)"
          type="number"
          min={0}
          placeholder="24"
          {...register('estimatedAgeMonths')}
          error={errors.estimatedAgeMonths?.message}
        />
        <Input
          label="Màu lông"
          placeholder="Vàng kem"
          maxLength={50}
          {...register('color')}
          error={errors.color?.message}
        />
        <Input
          label="Nhận diện"
          placeholder="Vết đốm tai trái, số microchip..."
          {...register('identificationNote')}
        />
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Textarea
          label="Dị ứng"
          placeholder="Thịt bò, thuốc kháng sinh..."
          {...register('allergyNote')}
        />
        <Textarea
          label="Nhu cầu dinh dưỡng"
          placeholder="Thức ăn khô hạt nhỏ, hạn chế muối..."
          {...register('nutritionNote')}
        />
        <Textarea
          label="Ghi chú đặc biệt"
          placeholder="Thói quen, tiền sử, lưu ý khi khám..."
          maxLength={500}
          {...register('specialNote')}
          error={errors.specialNote?.message}
        />
        <PetAvatarUpload
          previewUrl={avatarPreview}
          onPreviewChange={setAvatarPreview}
          onError={setAvatarError}
        />
      </div>

      {avatarError && (
        <p className="text-sm font-medium text-error-600">{avatarError}</p>
      )}

      {saveMutation.isError && (
        <p className="text-red-500 text-sm">Có lỗi xảy ra khi lưu thú cưng</p>
      )}

      <div className="mt-5 flex gap-2">
        <Button
          type="submit"
          disabled={saveMutation.isPending || speciesSelectOptions.length === 0}
        >
          {saveMutation.isPending
            ? 'Đang lưu...'
            : isEdit
              ? 'Lưu thay đổi'
              : 'Lưu'}
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={onCancel}
          disabled={saveMutation.isPending}
        >
          Hủy bỏ
        </Button>
      </div>
    </form>
  );
}
