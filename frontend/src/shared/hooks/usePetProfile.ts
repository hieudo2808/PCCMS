import { useQuery } from '@tanstack/react-query';
import { petApi } from '~/shared/api/petApi';
import { hasAccessToken } from '~/shared/auth/tokenStorage';

/** Hồ sơ nền thú cưng — dùng chung cho khám, làm đẹp, lưu trú, hóa đơn */
export function usePetProfile(petId: string | undefined) {
  return useQuery({
    queryKey: ['pet', petId],
    queryFn: () => petApi.getPetById(petId as string),
    enabled: Boolean(petId) && hasAccessToken(),
    staleTime: 60_000,
  });
}

export function useActivePets(ownerId?: string) {
  return useQuery({
    queryKey: ['pets', { isActive: true, ownerId }],
    queryFn: () =>
      petApi.getPets({
        ownerId,
        isActive: true,
        size: 100,
      }),
    enabled: hasAccessToken(),
    staleTime: 60_000,
  });
}
