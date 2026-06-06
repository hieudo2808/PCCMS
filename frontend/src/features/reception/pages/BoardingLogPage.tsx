import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Button, Select, Textarea } from '~/components/atoms';
import { Card, EmptyState } from '~/components/molecules';
import { boardingApi } from '~/shared/api/boardingApi';
import { hasAccessToken } from '~/shared/auth/tokenStorage';
import { useAuth } from '~/features/auth/context/AuthContext';
import {
  BOARDING_FEEDING_OPTIONS,
  BOARDING_HYGIENE_OPTIONS,
  BOARDING_PERIOD_OPTIONS,
  type StaffBoardingStay,
} from '~/types/boarding';

const emptyForm = {
  periodCode: 'MORNING' as const,
  feedingStatus: BOARDING_FEEDING_OPTIONS[0],
  hygieneStatus: BOARDING_HYGIENE_OPTIONS[0],
  healthNote: '',
  staffNote: '',
};

export function BoardingLogPage() {
  const queryClient = useQueryClient();
  const { isAuthenticated, user } = useAuth();
  const canFetch = isAuthenticated && hasAccessToken() && Boolean(user);

  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyForm);

  const { data: stays = [], isLoading } = useQuery({
    queryKey: ['boarding', 'staff', 'stays'],
    queryFn: () => boardingApi.getStaffActiveStays(),
    enabled: canFetch,
    refetchInterval: 30_000,
  });

  const selectedStay: StaffBoardingStay | null =
    stays.find((stay) => stay.sessionId === selectedSessionId) ?? stays[0] ?? null;

  useEffect(() => {
    if (!selectedSessionId && stays[0]) {
      setSelectedSessionId(stays[0].sessionId);
    }
  }, [stays, selectedSessionId]);

  const { data: todayLogs = [] } = useQuery({
    queryKey: ['boarding', 'staff', 'care-logs', selectedStay?.sessionId],
    queryFn: () => boardingApi.getStaffSessionLogs(selectedStay!.sessionId),
    enabled: canFetch && Boolean(selectedStay?.sessionId),
  });

  useEffect(() => {
    if (!selectedStay) {
      return;
    }

    const existing = todayLogs.find((log) => log.periodCode === form.periodCode);
    if (existing) {
      setForm({
        periodCode: existing.periodCode as typeof form.periodCode,
        feedingStatus: existing.feedingStatus,
        hygieneStatus: existing.hygieneStatus,
        healthNote: existing.healthNote ?? '',
        staffNote: existing.staffNote ?? '',
      });
      return;
    }

    setForm((current) => ({
      ...emptyForm,
      periodCode: current.periodCode,
    }));
  }, [selectedStay?.sessionId, todayLogs, form.periodCode]);

  const saveMutation = useMutation({
    mutationFn: () =>
      boardingApi.upsertStaffCareLog({
        sessionId: selectedStay!.sessionId,
        periodCode: form.periodCode,
        feedingStatus: form.feedingStatus,
        hygieneStatus: form.hygieneStatus,
        healthNote: form.healthNote || undefined,
        staffNote: form.staffNote || undefined,
      }),
    onSuccess: () => {
      toast.success('Lưu nhật ký thành công');
      queryClient.invalidateQueries({ queryKey: ['boarding', 'staff'] });
    },
    onError: () => toast.error('Không thể lưu nhật ký'),
  });

  if (isLoading) {
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
        description="Hiện không có thú cưng nào đang lưu trú tại trung tâm."
      />
    );
  }

  return (
    <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
      <Card title="Thú cưng đang lưu trú">
        <div className="space-y-3">
          {stays.map((stay) => {
            const isSelected = selectedStay?.sessionId === stay.sessionId;
            return (
              <button
                key={stay.sessionId}
                type="button"
                onClick={() => setSelectedSessionId(stay.sessionId)}
                className={`w-full rounded-3xl border p-4 text-left transition hover:border-emerald-300 hover:shadow-sm ${
                  isSelected ? 'border-emerald-300 bg-emerald-50/50' : 'border-slate-200'
                }`}
              >
                <p className="font-semibold">
                  {stay.roomLabel} — {stay.petName}
                </p>
                <p className="mt-1 text-sm text-slate-500">
                  Ngày {stay.currentDay}/{stay.totalDays}
                </p>
                <p className="mt-2 text-xs text-slate-500">{stay.todayLogSummary}</p>
              </button>
            );
          })}
        </div>
      </Card>

      <Card title="Cập nhật nhật ký hôm nay">
        {selectedStay && (
          <p className="mb-4 text-sm text-slate-500">
            {selectedStay.roomLabel} — {selectedStay.petName}
          </p>
        )}

        <div className="grid gap-4 md:grid-cols-2">
          <Select
            label="Buổi cập nhật"
            value={form.periodCode}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                periodCode: event.target.value as typeof form.periodCode,
              }))
            }
            options={BOARDING_PERIOD_OPTIONS.map((option) => ({
              value: option.value,
              label: option.label,
            }))}
          />
          <Select
            label="Tình trạng ăn uống"
            value={form.feedingStatus}
            onChange={(event) =>
              setForm((current) => ({ ...current, feedingStatus: event.target.value }))
            }
            options={[...BOARDING_FEEDING_OPTIONS]}
          />
          <Select
            label="Tình trạng vệ sinh"
            value={form.hygieneStatus}
            onChange={(event) =>
              setForm((current) => ({ ...current, hygieneStatus: event.target.value }))
            }
            options={[...BOARDING_HYGIENE_OPTIONS]}
          />
          <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 p-4">
            <p className="text-sm font-medium">Ảnh / video</p>
            <p className="mt-2 text-xs text-slate-500">
              Tối đa 5 tệp, mỗi tệp không quá 10MB.
            </p>
            <Button variant="outline" className="mt-3" disabled>
              Tải tệp lên
            </Button>
          </div>
        </div>

        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Textarea
            label="Ghi chú sức khỏe"
            placeholder="Biểu hiện bất thường, nhiệt độ, giờ uống thuốc..."
            value={form.healthNote}
            onChange={(event) =>
              setForm((current) => ({ ...current, healthNote: event.target.value }))
            }
          />
          <Textarea
            label="Ghi chú nhân viên"
            placeholder="Thói quen, tâm trạng, hoạt động trong ngày..."
            value={form.staffNote}
            onChange={(event) =>
              setForm((current) => ({ ...current, staffNote: event.target.value }))
            }
          />
        </div>

        <div className="mt-5 flex gap-2">
          <Button
            onClick={() => saveMutation.mutate()}
            disabled={!selectedStay || saveMutation.isPending}
          >
            {saveMutation.isPending ? 'Đang lưu...' : 'Lưu nhật ký'}
          </Button>
        </div>
      </Card>
    </div>
  );
}
