import { useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { Button, Tag } from '~/components/atoms';
import { Card, EmptyState, Modal } from '~/components/molecules';
import { petApi } from '~/shared/api/petApi';
import { petCatalogApi } from '~/shared/api/petCatalogApi';
import { PetForm } from '../components/PetForm';
import { PetDetailView } from '../components/PetDetailView';
import { PetAvatar } from '../components/PetAvatar';
import { PET_MESSAGES } from '../schema/petSchema';
import { useAuth } from '~/features/auth/context/AuthContext';
import { hasAccessToken } from '~/shared/auth/tokenStorage';
import type { PetResponse } from '~/types/pet';

type ActiveFilter = 'all' | 'active' | 'inactive';
type ModalMode = 'add' | 'edit' | 'detail' | null;

const FILTER_LABELS: Record<ActiveFilter, string> = {
  all: 'Tất cả',
  active: 'Đang hoạt động',
  inactive: 'Đã ẩn',
};

function toIsActiveParam(filter: ActiveFilter): boolean | undefined {
  if (filter === 'active') return true;
  if (filter === 'inactive') return false;
  return undefined;
}

function formatBreedSpecies(pet: PetResponse) {
  const breed = pet.breedName ?? pet.speciesName;
  return breed;
}

export function PetProfilesPage() {
  const queryClient = useQueryClient();
  const [modalMode, setModalMode] = useState<ModalMode>(null);
  const [selectedPetId, setSelectedPetId] = useState<string | null>(null);
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>('active');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [deactivateTarget, setDeactivateTarget] = useState<PetResponse | null>(null);
  const filterRef = useRef<HTMLDivElement>(null);

  const { isAuthenticated, user } = useAuth();
  const canFetch = isAuthenticated && hasAccessToken() && Boolean(user);
  const isActiveParam = toIsActiveParam(activeFilter);

  const { data: petsPage, isLoading, isError } = useQuery({
    queryKey: ['pets', { isActive: activeFilter }],
    queryFn: () => petApi.getPets(isActiveParam !== undefined ? { isActive: isActiveParam } : undefined),
    enabled: canFetch,
  });

  const { data: selectedPet } = useQuery({
    queryKey: ['pet', selectedPetId],
    queryFn: () => petApi.getPetById(selectedPetId as string),
    enabled: Boolean(selectedPetId) && modalMode === 'detail',
  });

  useQuery({
    queryKey: ['pet-catalog', 'species'],
    queryFn: () => petCatalogApi.getSpecies(),
    enabled: canFetch,
    staleTime: 5 * 60 * 1000,
  });

  const deactivateMutation = useMutation({
    mutationFn: (petId: string) => petApi.deletePet(petId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pets'] });
      toast.success(PET_MESSAGES.deleteSuccess);
      setDeactivateTarget(null);
      closeModal();
    },
    onError: () => {
      toast.error('Không thể xóa hồ sơ. Vui lòng thử lại.');
    },
  });

  const closeModal = () => {
    setModalMode(null);
    setSelectedPetId(null);
  };

  const openAdd = () => {
    setSelectedPetId(null);
    setModalMode('add');
  };

  const openDetail = (pet: PetResponse) => {
    setSelectedPetId(pet.id);
    setModalMode('detail');
  };

  const openEdit = (pet: PetResponse) => {
    setSelectedPetId(pet.id);
    setModalMode('edit');
  };

  const openDelete = (pet: PetResponse) => {
    setDeactivateTarget(pet);
  };

  const applyFilter = (filter: ActiveFilter) => {
    setActiveFilter(filter);
    setIsFilterOpen(false);
  };

  const modalTitle =
    modalMode === 'add'
      ? 'Đăng ký hồ sơ thú cưng'
      : modalMode === 'edit'
        ? 'Chỉnh sửa hồ sơ thú cưng'
        : modalMode === 'detail' && selectedPet
          ? `Chi tiết: ${selectedPet.name}`
          : '';

  const detailPet = selectedPet ?? petsPage?.content.find((p) => p.id === selectedPetId);

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 className="text-xl font-semibold tracking-tight">Hồ sơ thú cưng</h2>
          <p className="text-sm text-slate-500">
            Quản lý hồ sơ nền — khám bệnh, làm đẹp, lưu trú, hóa đơn
          </p>
        </div>
        <div className="flex gap-2">
          <div className="relative" ref={filterRef}>
            <Button
              variant="outline"
              onClick={() => setIsFilterOpen((open) => !open)}
              aria-expanded={isFilterOpen}
              aria-haspopup="listbox"
            >
              Bộ lọc: {FILTER_LABELS[activeFilter]}
            </Button>
            {isFilterOpen && (
              <div
                className="absolute right-0 z-20 mt-2 min-w-[180px] rounded-xl border border-slate-200 bg-white py-1 shadow-lg"
                role="listbox"
              >
                {(Object.keys(FILTER_LABELS) as ActiveFilter[]).map((key) => (
                  <button
                    key={key}
                    type="button"
                    role="option"
                    aria-selected={activeFilter === key}
                    className={`block w-full px-4 py-2 text-left text-sm hover:bg-slate-50 ${
                      activeFilter === key ? 'font-medium text-indigo-600' : 'text-slate-700'
                    }`}
                    onClick={() => applyFilter(key)}
                  >
                    {FILTER_LABELS[key]}
                  </button>
                ))}
              </div>
            )}
          </div>
          <Button onClick={openAdd}>
            <span className="inline-flex items-center gap-2">
              <Plus className="h-4 w-4" /> Thêm thú cưng mới
            </span>
          </Button>
        </div>
      </div>

      {isLoading && (
        <div className="flex justify-center p-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
        </div>
      )}

      {isError && (
        <EmptyState
          title="Không thể tải danh sách thú cưng"
          description="Vui lòng thử lại sau."
        />
      )}

      {!isLoading && !isError && petsPage?.content.length === 0 && (
        <EmptyState
          title={
            activeFilter === 'active' || activeFilter === 'all'
              ? PET_MESSAGES.emptyList
              : 'Không có kết quả phù hợp'
          }
          description={
            activeFilter === 'active' || activeFilter === 'all'
              ? 'Bấm nút bên dưới để đăng ký hồ sơ thú cưng đầu tiên.'
              : `Không có thú cưng nào ở trạng thái "${FILTER_LABELS[activeFilter]}".`
          }
          action={
            (activeFilter === 'active' || activeFilter === 'all') && (
              <Button onClick={openAdd}>Thêm mới ngay</Button>
            )
          }
        />
      )}

      {!isLoading && !isError && petsPage?.content && petsPage.content.length > 0 && (
        <div className="grid gap-4 lg:grid-cols-3">
          {petsPage.content.map((pet) => (
            <Card key={pet.id} className="overflow-hidden p-0 relative">
              <PetAvatar pet={pet} />
              <div className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-semibold">{pet.name}</h3>
                    <p className="text-sm text-slate-500">{formatBreedSpecies(pet)}</p>
                  </div>
                  <Tag tone={pet.isActive ? 'green' : 'default'}>
                    {pet.isActive ? 'Đang hoạt động' : 'Đã ẩn'}
                  </Tag>
                </div>
                <div className="mt-4 rounded-2xl bg-slate-50 p-3 text-sm">
                  <p className="text-slate-500">Cân nặng</p>
                  <p className="mt-1 font-medium">
                    {pet.weightKg != null ? `${pet.weightKg} kg` : 'Chưa cập nhật'}
                  </p>
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  <Button variant="outline" className="flex-1 min-w-[120px]" onClick={() => openDetail(pet)}>
                    Xem chi tiết
                  </Button>
                  <Button
                    variant="ghost"
                    className="flex-1 min-w-[120px]"
                    onClick={() => openEdit(pet)}
                    disabled={!pet.isActive}
                  >
                    Chỉnh sửa
                  </Button>
                  {pet.isActive && (
                    <Button
                      variant="outline"
                      className="w-full border-red-200 text-red-700 hover:bg-red-50"
                      onClick={() => openDelete(pet)}
                    >
                      Xóa hồ sơ
                    </Button>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        isOpen={modalMode === 'add' || modalMode === 'edit'}
        onClose={closeModal}
        title={modalTitle}
      >
        <PetForm
          petId={modalMode === 'edit' ? selectedPetId ?? undefined : undefined}
          onSuccess={closeModal}
          onCancel={closeModal}
        />
      </Modal>

      <Modal
        isOpen={modalMode === 'detail' && detailPet != null}
        onClose={closeModal}
        title={modalTitle}
      >
        {detailPet && (
          <PetDetailView
            pet={detailPet}
            onEdit={() => setModalMode('edit')}
            onDeactivate={() => openDelete(detailPet)}
            isDeactivating={deactivateMutation.isPending}
            onClose={closeModal}
          />
        )}
      </Modal>

      <Modal
        isOpen={deactivateTarget != null}
        onClose={() => !deactivateMutation.isPending && setDeactivateTarget(null)}
        title="Xác nhận xóa hồ sơ"
      >
        {deactivateTarget && (
          <div className="space-y-4">
            <p className="text-sm text-slate-600">
              {PET_MESSAGES.deleteConfirm(deactivateTarget.name)}
            </p>
            <div className="flex gap-2">
              <Button
                className="bg-red-600 hover:bg-red-700 text-white"
                onClick={() => deactivateMutation.mutate(deactivateTarget.id)}
                disabled={deactivateMutation.isPending}
              >
                {deactivateMutation.isPending ? 'Đang xử lý...' : 'Xác nhận xóa'}
              </Button>
              <Button
                variant="outline"
                onClick={() => setDeactivateTarget(null)}
                disabled={deactivateMutation.isPending}
              >
                Hủy bỏ
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
