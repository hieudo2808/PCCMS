import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, Play, Scissors, XCircle } from "lucide-react";
import { useState } from "react";
import toast from "react-hot-toast";
import { Button, Tag } from "~/components/atoms";
import { Card, EmptyState, SectionTitle, SummaryRow } from "~/components/molecules";
import { groomingApi } from "~/features/grooming/api/groomingApi";
import type { GroomingStatus, GroomingTicketResponse } from "~/types/grooming";

const columns: Array<{ title: string; statuses: GroomingStatus[]; tone: "amber" | "blue" | "green" | "red" }> = [
  { title: "Chờ duyệt", statuses: ["PENDING"], tone: "amber" },
  { title: "Đã xác nhận", statuses: ["CONFIRMED"], tone: "blue" },
  { title: "Đang làm", statuses: ["IN_SERVICE"], tone: "blue" },
  { title: "Đã đóng", statuses: ["COMPLETED", "CANCELLED"], tone: "green" },
];

function formatCurrency(value?: number) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value ?? 0);
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("vi-VN");
}

export function GroomingBoardPage() {
  const queryClient = useQueryClient();
  const [stationByTicket, setStationByTicket] = useState<Record<string, string>>({});

  const ticketsQuery = useQuery({
    queryKey: ["staff-grooming-tickets"],
    queryFn: () => groomingApi.getTickets(),
  });

  const stationsQuery = useQuery({
    queryKey: ["grooming-stations"],
    queryFn: () => groomingApi.getStations(),
  });

  const refreshBoard = () => {
    queryClient.invalidateQueries({ queryKey: ["staff-grooming-tickets"] });
  };

  const confirmMutation = useMutation({
    mutationFn: ({ ticketId, stationId }: { ticketId: string; stationId: string }) =>
      groomingApi.confirmTicket(ticketId, stationId),
    onSuccess: () => {
      toast.success("Đã xác nhận lịch làm đẹp");
      refreshBoard();
    },
    onError: () => toast.error("Không thể xác nhận lịch làm đẹp"),
  });

  const startMutation = useMutation({
    mutationFn: (ticketId: string) => groomingApi.startTicket(ticketId),
    onSuccess: () => {
      toast.success("Đã bắt đầu dịch vụ");
      refreshBoard();
    },
    onError: () => toast.error("Không thể bắt đầu dịch vụ"),
  });

  const completeMutation = useMutation({
    mutationFn: (ticketId: string) => groomingApi.completeTicket(ticketId, "Hoàn thành dịch vụ làm đẹp"),
    onSuccess: (ticket) => {
      toast.success(ticket.invoice ? `Đã tạo hóa đơn ${ticket.invoice.invoiceCode}` : "Đã hoàn thành dịch vụ");
      refreshBoard();
    },
    onError: () => toast.error("Không thể hoàn thành dịch vụ"),
  });

  const cancelMutation = useMutation({
    mutationFn: (ticketId: string) => groomingApi.cancelTicket(ticketId, "Nhân viên hủy lịch theo điều phối"),
    onSuccess: () => {
      toast.success("Đã hủy lịch làm đẹp");
      refreshBoard();
    },
    onError: () => toast.error("Không thể hủy lịch làm đẹp"),
  });

  if (ticketsQuery.isLoading || stationsQuery.isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-slate-500">
        <Loader2 className="h-4 w-4 animate-spin" /> Đang tải bảng làm đẹp
      </div>
    );
  }

  if (ticketsQuery.isError || stationsQuery.isError) {
    return <EmptyState title="Không thể tải bảng làm đẹp" description="Vui lòng thử lại sau." />;
  }

  const tickets = ticketsQuery.data?.content ?? [];
  const stations = stationsQuery.data ?? [];

  return (
    <div className="space-y-6">
      <SectionTitle title="Bảng trạng thái dịch vụ làm đẹp" />
      {tickets.length === 0 ? (
        <EmptyState title="Chưa có lịch làm đẹp" description="Các yêu cầu mới của chủ nuôi sẽ hiển thị tại đây." />
      ) : (
        <div className="grid gap-4 xl:grid-cols-4">
          {columns.map((column) => {
            const columnTickets = tickets.filter((ticket) => column.statuses.includes(ticket.statusCode));
            return (
              <Card key={column.title} title={column.title} right={<Tag tone={column.tone}>{columnTickets.length} phiếu</Tag>}>
                <div className="space-y-3">
                  {columnTickets.length === 0 ? (
                    <p className="text-sm text-slate-500">Không có phiếu.</p>
                  ) : (
                    columnTickets.map((ticket) => (
                      <TicketCard
                        key={ticket.id}
                        ticket={ticket}
                        stations={stations}
                        selectedStationId={stationByTicket[ticket.id] ?? ""}
                        onStationChange={(stationId) => setStationByTicket((prev) => ({ ...prev, [ticket.id]: stationId }))}
                        onConfirm={() => {
                          const stationId = stationByTicket[ticket.id];
                          if (!stationId) {
                            toast.error("Chọn khu làm đẹp trước khi xác nhận");
                            return;
                          }
                          confirmMutation.mutate({ ticketId: ticket.id, stationId });
                        }}
                        onStart={() => startMutation.mutate(ticket.id)}
                        onComplete={() => completeMutation.mutate(ticket.id)}
                        onCancel={() => cancelMutation.mutate(ticket.id)}
                        isPending={
                          confirmMutation.isPending ||
                          startMutation.isPending ||
                          completeMutation.isPending ||
                          cancelMutation.isPending
                        }
                      />
                    ))
                  )}
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}

interface TicketCardProps {
  ticket: GroomingTicketResponse;
  stations: Array<{ id: string; stationCode: string; name: string }>;
  selectedStationId: string;
  onStationChange: (stationId: string) => void;
  onConfirm: () => void;
  onStart: () => void;
  onComplete: () => void;
  onCancel: () => void;
  isPending: boolean;
}

function TicketCard({
  ticket,
  stations,
  selectedStationId,
  onStationChange,
  onConfirm,
  onStart,
  onComplete,
  onCancel,
  isPending,
}: TicketCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <p className="font-semibold text-slate-900">{ticket.petName}</p>
          <p className="text-sm text-slate-500">{ticket.serviceName}</p>
        </div>
        <Tag tone={ticket.statusCode === "CANCELLED" ? "red" : ticket.statusCode === "COMPLETED" ? "green" : "amber"}>
          {ticket.statusCode}
        </Tag>
      </div>

      <div className="space-y-2 text-sm">
        <SummaryRow label="Chủ nuôi" value={ticket.ownerName} />
        <SummaryRow label="Thời gian" value={formatDateTime(ticket.scheduledStartAt)} />
        <SummaryRow label="Giá" value={formatCurrency(ticket.basePriceVnd)} />
        <SummaryRow label="Station" value={ticket.stationName ?? "Chưa gán"} />
      </div>

      {ticket.statusCode === "PENDING" && (
        <div className="mt-4 space-y-3">
          <select
            value={selectedStationId}
            onChange={(event) => onStationChange(event.target.value)}
            className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
          >
            <option value="">Chọn khu làm đẹp</option>
            {stations.map((station) => (
              <option key={station.id} value={station.id}>
                {station.stationCode} - {station.name}
              </option>
            ))}
          </select>
          <div className="flex flex-wrap gap-2">
            <Button disabled={isPending} onClick={onConfirm}>
              <span className="inline-flex items-center gap-2">
                <Scissors className="h-4 w-4" /> Xác nhận
              </span>
            </Button>
            <Button variant="outline" disabled={isPending} onClick={onCancel}>
              <span className="inline-flex items-center gap-2">
                <XCircle className="h-4 w-4" /> Hủy
              </span>
            </Button>
          </div>
        </div>
      )}

      {ticket.statusCode === "CONFIRMED" && (
        <div className="mt-4 flex flex-wrap gap-2">
          <Button disabled={isPending} onClick={onStart}>
            <span className="inline-flex items-center gap-2">
              <Play className="h-4 w-4" /> Bắt đầu
            </span>
          </Button>
          <Button variant="outline" disabled={isPending} onClick={onCancel}>
            Hủy
          </Button>
        </div>
      )}

      {ticket.statusCode === "IN_SERVICE" && (
        <Button className="mt-4" disabled={isPending} onClick={onComplete}>
          Hoàn thành và tạo hóa đơn
        </Button>
      )}

      {ticket.invoice && (
        <div className="mt-3 rounded-xl bg-white p-3 text-sm text-emerald-800">
          Hóa đơn {ticket.invoice.invoiceCode}: {formatCurrency(ticket.invoice.totalAmountVnd)} chờ thanh toán
        </div>
      )}
    </div>
  );
}
