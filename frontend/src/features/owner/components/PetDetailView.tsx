import { Button, Tag } from '~/components/atoms';
import type { PetResponse } from '~/types/pet';

function formatSex(sex: PetResponse['sex']) {
  if (sex === 'MALE') return 'Đực';
  if (sex === 'FEMALE') return 'Cái';
  return 'Chưa rõ';
}

function formatAge(pet: PetResponse) {
  if (pet.birthDate) {
    return new Date(pet.birthDate).toLocaleDateString('vi-VN');
  }
  if (pet.estimatedAgeMonths != null) {
    return `${pet.estimatedAgeMonths} tháng (ước tính)`;
  }
  return 'Chưa cập nhật';
}

function display(value?: string | null) {
  return value?.trim() ? value : 'Chưa cập nhật';
}

interface PetDetailViewProps {
  pet: PetResponse;
  onEdit: () => void;
  onDeactivate?: () => void;
  isDeactivating?: boolean;
  onClose: () => void;
}

export function PetDetailView({
  pet,
  onEdit,
  onDeactivate,
  isDeactivating,
  onClose,
}: PetDetailViewProps) {
  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-2xl font-semibold">{pet.name}</h3>
          <p className="mt-1 text-sm text-slate-500">
            {pet.speciesName}
            {pet.breedName ? ` • ${pet.breedName}` : ''}
          </p>
        </div>
        <Tag tone={pet.isActive ? 'green' : 'default'}>
          {pet.isActive ? 'Đang hoạt động' : 'Đã ẩn'}
        </Tag>
      </div>

      <section className="space-y-3">
        <h4 className="text-sm font-semibold text-slate-700">Thông tin nhận diện</h4>
        <div className="grid gap-3 sm:grid-cols-2">
          <DetailItem label="Loài" value={pet.speciesName} />
          <DetailItem label="Giống" value={display(pet.breedName)} />
          <DetailItem label="Giới tính" value={formatSex(pet.sex)} />
          <DetailItem label="Ngày sinh / Tuổi" value={formatAge(pet)} />
          <DetailItem
            label="Cân nặng"
            value={pet.weightKg != null ? `${pet.weightKg} kg` : 'Chưa cập nhật'}
          />
          <DetailItem label="Màu lông" value={display(pet.color)} />
          <DetailItem label="Nhận diện" value={display(pet.identificationNote)} className="sm:col-span-2" />
        </div>
      </section>

      <section className="space-y-3">
        <h4 className="text-sm font-semibold text-slate-700">Ghi chú y tế & chăm sóc</h4>
        <div className="space-y-3">
          <NoteBlock label="Dị ứng" value={display(pet.allergyNote)} highlight={Boolean(pet.allergyNote?.trim())} />
          <NoteBlock label="Nhu cầu dinh dưỡng" value={display(pet.nutritionNote)} />
          <NoteBlock label="Ghi chú đặc biệt" value={display(pet.specialNote)} />
        </div>
      </section>

      <div className="flex flex-wrap gap-2 pt-2">
        {pet.isActive && onDeactivate && (
          <Button
            variant="outline"
            className="border-red-200 text-red-700 hover:bg-red-50"
            onClick={onDeactivate}
            disabled={isDeactivating}
          >
            {isDeactivating ? 'Đang xử lý...' : 'Ngừng hoạt động'}
          </Button>
        )}
        <Button onClick={onEdit} disabled={!pet.isActive}>
          Chỉnh sửa
        </Button>
        <Button variant="outline" onClick={onClose}>
          Đóng
        </Button>
      </div>

      {!pet.isActive && (
        <p className="text-sm text-slate-500">
          Hồ sơ đã ngừng hoạt động — không dùng cho đặt lịch hoặc dịch vụ mới.
        </p>
      )}
    </div>
  );
}

function DetailItem({
  label,
  value,
  className = '',
}: {
  label: string;
  value: string;
  className?: string;
}) {
  return (
    <div className={`rounded-2xl border border-slate-100 bg-white p-4 ${className}`}>
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-1 font-medium text-slate-900">{value}</p>
    </div>
  );
}

function NoteBlock({
  label,
  value,
  highlight = false,
}: {
  label: string;
  value: string;
  highlight?: boolean;
}) {
  return (
    <div
      className={`rounded-2xl p-4 ${
        highlight ? 'bg-amber-50 border border-amber-100' : 'bg-slate-50'
      }`}
    >
      <p className="text-sm font-medium text-slate-700">{label}</p>
      <p className="mt-1 text-sm text-slate-600 whitespace-pre-wrap">{value}</p>
    </div>
  );
}
