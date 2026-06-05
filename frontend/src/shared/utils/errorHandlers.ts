import { AxiosError } from 'axios';
import type { ApiResponse } from '../../types';

const apiErrorMessages: Record<string, string> = {
  ERR_BOARDING_006_DUPLICATE_BOOKING:
    'Thú cưng này đã có yêu cầu lưu trú trùng khoảng thời gian. Vui lòng xem Theo dõi lưu trú hoặc chọn thời gian khác.',
  ERR_GROOMING_009_DUPLICATE_BOOKING:
    'Thú cưng này đã có yêu cầu làm đẹp trùng thời gian. Vui lòng xem Theo dõi làm đẹp hoặc chọn thời gian khác.',
};

export function parseApiError(error: unknown): string {
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as AxiosError<ApiResponse<unknown>>;
    const data = axiosError.response?.data;
    if (data && typeof data === 'object') {
      if (data.errorCode && apiErrorMessages[data.errorCode]) {
        return apiErrorMessages[data.errorCode];
      }
      if (data.message) return data.message;
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Đã xảy ra lỗi không xác định';
}
