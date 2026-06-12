import type { PageResponse } from '~/types/api';

/** Spring Data dùng page 0-based; UI admin thường dùng 1-based */
export function toSpringPage(uiPage = 1): number {
  return Math.max(0, uiPage - 1);
}

/** Backend bọc PageResponse trong ApiResponse → sau unwrap còn lớp data.content */
export function normalizePage<T>(raw: unknown): PageResponse<T> {
  const empty: PageResponse<T> = {
    content: [],
    pageNumber: 1,
    pageSize: 0,
    totalElements: 0,
    totalPages: 0,
    isLast: true,
  };

  if (!raw || typeof raw !== 'object') {
    return empty;
  }

  const record = raw as PageResponse<T> & { data?: PageResponse<T> & { data?: PageResponse<T> } };

  if (Array.isArray(record.content)) {
    return record;
  }

  if (record.data && Array.isArray(record.data.content)) {
    return record.data;
  }

  if (record.data?.data && Array.isArray(record.data.data.content)) {
    return record.data.data;
  }

  return empty;
}
