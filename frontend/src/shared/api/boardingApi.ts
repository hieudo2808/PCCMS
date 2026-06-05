import axiosClient from '~/shared/api/axiosClient';
import type { BoardingStay, CareLogEntry } from '~/types/boarding';

export const boardingApi = {
  getActiveStays: (): Promise<BoardingStay[]> => {
    return axiosClient.get('/v1/boarding/owner/stays');
  },

  getCareLogs: (petId?: string): Promise<CareLogEntry[]> => {
    return axiosClient.get('/v1/boarding/owner/care-logs', {
      params: petId ? { petId } : undefined,
    });
  },
};
