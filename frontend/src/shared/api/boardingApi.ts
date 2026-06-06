import axiosClient from '~/shared/api/axiosClient';
import type {
  BoardingStay,
  CareLogEntry,
  StaffBoardingStay,
  UpsertCareLogPayload,
} from '~/types/boarding';

export const boardingApi = {
  getActiveStays: (): Promise<BoardingStay[]> => {
    return axiosClient.get('/v1/boarding/owner/stays');
  },

  getCareLogs: (petId?: string): Promise<CareLogEntry[]> => {
    return axiosClient.get('/v1/boarding/owner/care-logs', {
      params: petId ? { petId } : undefined,
    });
  },

  getStaffActiveStays: (): Promise<StaffBoardingStay[]> => {
    return axiosClient.get('/v1/boarding/staff/stays');
  },

  getStaffSessionLogs: (sessionId: string, logDate?: string): Promise<CareLogEntry[]> => {
    return axiosClient.get('/v1/boarding/staff/care-logs', {
      params: { sessionId, logDate },
    });
  },

  upsertStaffCareLog: (payload: UpsertCareLogPayload): Promise<CareLogEntry> => {
    return axiosClient.post('/v1/boarding/staff/care-logs', payload);
  },
};
