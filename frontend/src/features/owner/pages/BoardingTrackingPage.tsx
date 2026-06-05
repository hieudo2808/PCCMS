import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Tag } from '~/components/atoms';
import { Card, EmptyState } from '~/components/molecules';
import { boardingApi } from '~/shared/api/boardingApi';
import { PET_MESSAGES } from '../schema/petSchema';
import type { CareLogEntry } from '~/types/boarding';
import { hasAccessToken } from '~/shared/auth/tokenStorage';
import { useAuth } from '~/features/auth/context/AuthContext';

function formatLogTitle(log: CareLogEntry) {
  const date = new Date(log.logDate).toLocaleDateString('vi-VN');
  return `${date} • ${log.periodLabel}`;
}

export function BoardingTrackingPage() {
  const { isAuthenticated, user } = useAuth();
  const canFetch = isAuthenticated && hasAccessToken() && Boolean(user);
  const [selectedLogId, setSelectedLogId] = useState<string | null>(null);
  const [petFilter, setPetFilter] = useState<string>('');

  const { data: stays = [], isLoading: staysLoading } = useQuery({
    queryKey: ['boarding', 'stays'],
    queryFn: () => boardingApi.getActiveStays(),
    enabled: canFetch,
  });

  const { data: careLogs = [], isLoading: logsLoading } = useQuery({
    queryKey: ['boarding', 'care-logs', petFilter],
    queryFn: () => boardingApi.getCareLogs(petFilter || undefined),
    enabled: canFetch && stays.length > 0,
  });

  const selectedLog = careLogs.find((log) => log.id === selectedLogId) ?? careLogs[0] ?? null;

  if (staysLoading) {
    return (
      <div className="flex justify-center p-12">
        <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-indigo-600" />
      </div>
    );
  }

  if (stays.length === 0) {
    return (
      <EmptyState
        title="Thú cưng đang lưu trú"
        description={PET_MESSAGES.noBoardingPets}
      />
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold tracking-tight">Thú cưng đang lưu trú</h2>
        <p className="text-sm text-slate-500">Theo dõi nhật ký chăm sóc hàng ngày tại trung tâm</p>
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setPetFilter('')}
          className={`rounded-full px-4 py-1.5 text-sm ${
            petFilter === '' ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-700'
          }`}
        >
          Tất cả
        </button>
        {stays.map((stay) => (
          <button
            key={stay.petId}
            type="button"
            onClick={() => setPetFilter(stay.petId)}
            className={`rounded-full px-4 py-1.5 text-sm ${
              petFilter === stay.petId ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-700'
            }`}
          >
            {stay.petName}
          </button>
        ))}
      </div>

      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <Card title="Nhật ký lưu trú">
          {logsLoading && (
            <div className="flex justify-center py-8">
              <div className="h-6 w-6 animate-spin rounded-full border-b-2 border-indigo-600" />
            </div>
          )}

          {!logsLoading && careLogs.length === 0 && (
            <p className="py-8 text-center text-sm text-slate-500">
              {PET_MESSAGES.careLogsUpdating}
            </p>
          )}

          {!logsLoading && careLogs.length > 0 && (
            <div className="space-y-3">
              {careLogs.map((log) => (
                <button
                  key={log.id}
                  type="button"
                  onClick={() => setSelectedLogId(log.id)}
                  className={`w-full rounded-3xl border p-4 text-left transition hover:border-slate-300 hover:shadow-sm ${
                    selectedLog?.id === log.id ? 'border-emerald-300 bg-emerald-50/50' : 'border-slate-200'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h4 className="font-semibold">{formatLogTitle(log)}</h4>
                      <p className="mt-1 text-sm text-slate-500">
                        {log.petName} — Ăn: {log.feedingStatus} • Vệ sinh: {log.hygieneStatus}
                      </p>
                      {log.mediaCaptions.length > 0 && (
                        <p className="mt-2 text-xs text-slate-500">
                          {log.mediaCaptions.length} tệp đính kèm
                        </p>
                      )}
                    </div>
                    <Tag tone="blue">Nhật ký</Tag>
                  </div>
                </button>
              ))}
            </div>
          )}
        </Card>

        <Card title="Chi tiết nhật ký">
          {!selectedLog ? (
            <p className="py-8 text-center text-sm text-slate-500">
              {PET_MESSAGES.careLogsUpdating}
            </p>
          ) : (
            <>
              <p className="mb-4 text-sm text-slate-500">
                {selectedLog.petName} — {formatLogTitle(selectedLog)}
              </p>

              {selectedLog.mediaCaptions.length > 0 ? (
                <div className="grid gap-4 md:grid-cols-3">
                  {selectedLog.mediaCaptions.map((caption) => (
                    <div
                      key={caption}
                      className="flex h-36 items-center justify-center rounded-3xl bg-linear-to-br from-amber-100 to-orange-50 text-xs text-slate-500"
                    >
                      {caption}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="grid gap-4 md:grid-cols-3">
                  <div className="h-36 rounded-3xl bg-linear-to-br from-amber-100 to-orange-50" />
                  <div className="h-36 rounded-3xl bg-linear-to-br from-sky-100 to-cyan-50" />
                  <div className="h-36 rounded-3xl bg-linear-to-br from-emerald-100 to-lime-50" />
                </div>
              )}

              <div className="mt-5 grid gap-4 md:grid-cols-2">
                <div className="rounded-3xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Tình trạng ăn uống</p>
                  <p className="mt-1 font-semibold">{selectedLog.feedingStatus}</p>
                </div>
                <div className="rounded-3xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Tình trạng vệ sinh</p>
                  <p className="mt-1 font-semibold">{selectedLog.hygieneStatus}</p>
                </div>
              </div>

              {selectedLog.healthNote && (
                <div className="mt-4 rounded-3xl bg-amber-50 p-4 text-sm text-amber-900">
                  Ghi chú sức khỏe: {selectedLog.healthNote}
                </div>
              )}

              {selectedLog.staffNote && (
                <div className="mt-4 rounded-3xl bg-emerald-50 p-4 text-sm text-emerald-900">
                  Ghi chú của nhân viên: {selectedLog.staffNote}
                </div>
              )}
            </>
          )}
        </Card>
      </div>
    </div>
  );
}
