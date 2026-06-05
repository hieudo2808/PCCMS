import { AlertTriangle, Loader2 } from 'lucide-react';
import { Tag } from '~/components/atoms';
import { usePetProfile } from '~/shared/hooks/usePetProfile';
import type { PetResponse } from '~/types/pet';

function formatSex(sex: PetResponse['sex']) {
  if (sex === 'MALE') return 'Đực';
  if (sex === 'FEMALE') return 'Cái';
  return 'Chưa rõ';
}

function orEmpty(value: string | number | null | undefined, suffix = '') {
  if (value == null || value === '') return 'Chưa cập nhật';
  return `${value}${suffix}`;
}

interface PetProfileSummaryProps {
  petId: string;
  /** Hiển thị dị ứng / dinh dưỡng / ghi chú — hữu ích khi tiếp nhận khám */
  showClinicalNotes?: boolean;
  className?: string;
}

/** Khối hồ sơ nền dùng chung cho subsystem (khám, spa, lưu trú, hóa đơn) */
export function PetProfileSummary({
  petId,
  showClinicalNotes = true,
  className = '',
}: PetProfileSummaryProps) {
  const { data: pet, isLoading, isError } = usePetProfile(petId);

  if (isLoading) {
    return (
      <div className={`flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-500 ${className}`}>
        <Loader2 className="h-4 w-4 animate-spin" />
        Đang tải hồ sơ thú cưng...
      </div>
    );
  }

  if (isError || !pet) {
    return (
      <div className={`rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 ${className}`}>
        Không tải được hồ sơ thú cưng.
      </div>
    );
  }

  const hasAllergy = Boolean(pet.allergyNote?.trim());

  return (
    <div className={`rounded-2xl border border-slate-200 bg-white p-4 space-y-4 ${className}`}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Hồ sơ nền</p>
          <h3 className="text-lg font-semibold text-slate-900">{pet.name}</h3>
          <p className="text-sm text-slate-500">
            {pet.speciesName}
            {pet.breedName ? ` • ${pet.breedName}` : ''}
          </p>
        </div>
        <Tag tone={pet.isActive ? 'green' : 'default'}>
          {pet.isActive ? 'Đang hoạt động' : 'Đã ẩn'}
        </Tag>
      </div>

      <div className="grid gap-2 sm:grid-cols-2 text-sm">
        <SummaryRow label="Giới tính" value={formatSex(pet.sex)} />
        <SummaryRow label="Cân nặng" value={orEmpty(pet.weightKg, ' kg')} />
        <SummaryRow label="Màu lông" value={orEmpty(pet.color)} />
        <SummaryRow label="Nhận diện" value={orEmpty(pet.identificationNote)} />
      </div>

      {showClinicalNotes && (
        <div className="space-y-2 border-t border-slate-100 pt-3">
          {hasAllergy && (
            <div className="flex gap-2 rounded-xl bg-amber-50 p-3 text-sm text-amber-900">
              <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
              <div>
                <p className="font-medium">Dị ứng</p>
                <p className="mt-0.5 whitespace-pre-wrap">{pet.allergyNote}</p>
              </div>
            </div>
          )}
          <NoteBlock label="Nhu cầu dinh dưỡng" value={pet.nutritionNote} />
          <NoteBlock label="Ghi chú đặc biệt" value={pet.specialNote} />
        </div>
      )}
    </div>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-slate-50 px-3 py-2">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="mt-0.5 font-medium text-slate-800">{value}</p>
    </div>
  );
}

function NoteBlock({ label, value }: { label: string; value?: string }) {
  if (!value?.trim()) return null;
  return (
    <div className="rounded-xl bg-slate-50 p-3 text-sm">
      <p className="font-medium text-slate-700">{label}</p>
      <p className="mt-1 text-slate-600 whitespace-pre-wrap">{value}</p>
    </div>
  );
}
