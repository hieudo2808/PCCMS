import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, ReceiptText, XCircle } from "lucide-react";
import toast from "react-hot-toast";
import { Button, Tag } from "~/components/atoms";
import { Card, EmptyState, SummaryRow } from "~/components/molecules";
import { groomingApi } from "~/features/grooming/api/groomingApi";
import type { GroomingStatus } from "~/types/grooming";

const statusLabels: Record<GroomingStatus, string> = {
  PENDING: "Chờ duyệt",
  CONFIRMED: "Đã xác nhận",
  IN_SERVICE: "Đang làm",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

const statusTones: Record<GroomingStatus, "amber" | "blue" | "green" | "red"> = {
  PENDING: "amber",
  CONFIRMED: "blue",
  IN_SERVICE: "blue",
  COMPLETED: "green",
  CANCELLED: "red",
};

function formatCurrency(value?: number) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value ?? 0);
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("vi-VN");
}

export function GroomingTrackingPage() {
  const queryClient = useQueryClient();
  const ticketsQuery = useQuery({
    queryKey: ["my-grooming-tickets"],
    queryFn: () => groomingApi.getMyTickets(),
  });
  const cancelMutation = useMutation({
    mutationFn: (ticketId: string) => groomingApi.cancelTicket(ticketId, "Chủ nuôi hủy lịch trước khi nhân viên duyệt"),
    onSuccess: () => {
      toast.success("Đã hủy lịch làm đẹp");
      queryClient.invalidateQueries({ queryKey: ["my-grooming-tickets"] });
    },
    onError: () => toast.error("Không thể hủy lịch làm đẹp"),
  });

  if (ticketsQuery.isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-slate-500">
        <Loader2 className="h-4 w-4 animate-spin" /> Đang tải lịch làm đẹp
      </div>
    );
  }

  if (ticketsQuery.isError) {
    return <EmptyState title="Không thể tải lịch làm đẹp" description="Vui lòng thử lại sau." />;
  }

  const tickets = ticketsQuery.data?.content ?? [];

  if (tickets.length === 0) {
    return <EmptyState title="Chưa có lịch làm đẹp" description="Các yêu cầu làm đẹp của bạn sẽ hiển thị tại đây." />;
  }

  return (
    <div className="space-y-4">
      {tickets.map((ticket) => (
        <Card
          key={ticket.id}
          title={`${ticket.petName} - ${ticket.serviceName}`}
          subtitle={`${formatDateTime(ticket.scheduledStartAt)} đến ${formatDateTime(ticket.scheduledEndAt)}`}
          right={<Tag tone={statusTones[ticket.statusCode]}>{statusLabels[ticket.statusCode]}</Tag>}
        >
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <SummaryRow label="Mã yêu cầu" value={ticket.orderCode} />
              <SummaryRow label="Khu làm đẹp" value={ticket.stationName ?? "Nhân viên chưa sắp xếp"} />
              <SummaryRow label="Nhân viên" value={ticket.assignedStaffName ?? "Chưa phân công"} />
              <SummaryRow label="Ghi chú" value={ticket.ownerNote || "-"} />
              {ticket.statusCode === "PENDING" && (
                <Button
                  variant="outline"
                  disabled={cancelMutation.isPending}
                  onClick={() => cancelMutation.mutate(ticket.id)}
                >
                  <span className="inline-flex items-center gap-2">
                    <XCircle className="h-4 w-4" /> Hủy lịch
                  </span>
                </Button>
              )}
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-800">
                <ReceiptText className="h-4 w-4" /> Hóa đơn
              </div>
              {ticket.invoice ? (
                <div className="space-y-2">
                  <SummaryRow label="Mã hóa đơn" value={ticket.invoice.invoiceCode} />
                  <SummaryRow label="Trạng thái" value={ticket.invoice.statusCode === "UNPAID" ? "Chờ thanh toán" : ticket.invoice.statusCode} />
                  <SummaryRow label="Tổng tiền" value={formatCurrency(ticket.invoice.totalAmountVnd)} />
                </div>
              ) : (
                <p className="text-sm text-slate-500">Hóa đơn sẽ xuất hiện sau khi dịch vụ hoàn thành.</p>
              )}
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
}
