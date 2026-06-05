import { Select } from '~/components/atoms';
import { useActivePets } from '~/shared/hooks/usePetProfile';

interface PetSelectProps {
  value: string;
  onChange: (petId: string) => void;
  ownerId?: string;
  label?: string;
  placeholder?: string;
  required?: boolean;
  error?: string;
  disabled?: boolean;
}

/** Chọn thú cưng đang hoạt động — dùng khi đặt lịch / tạo dịch vụ */
export function PetSelect({
  value,
  onChange,
  ownerId,
  label = 'Thú cưng',
  placeholder = 'Chọn thú cưng',
  required,
  error,
  disabled,
}: PetSelectProps) {
  const { data: petsPage, isLoading, isError } = useActivePets(ownerId);

  const options = (petsPage?.content ?? []).map((pet) => ({
    value: pet.id,
    label: `${pet.name} (${pet.speciesName}${pet.breedName ? ` • ${pet.breedName}` : ''})`,
  }));

  return (
    <Select
      label={label}
      required={required}
      placeholder={
        isLoading
          ? 'Đang tải...'
          : options.length === 0
            ? 'Chưa có thú cưng đang hoạt động'
            : placeholder
      }
      options={options}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      error={error ?? (isError ? 'Không tải được danh sách thú cưng' : undefined)}
      disabled={disabled || isLoading || options.length === 0}
    />
  );
}
