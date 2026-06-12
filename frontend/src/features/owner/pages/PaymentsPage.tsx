import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Clock3, CreditCard, Scissors } from "lucide-react";
import toast from "react-hot-toast";
import { Button, Input, Tag } from "~/components/atoms";
import { Card, DataTable, EmptyState, MiniGridStats, Modal } from "~/components/molecules";
import { invoiceApi } from "~/shared/api/invoiceApi";
import { paymentApi, type PaymentMethod } from "~/shared/api/paymentApi";
import type { InvoiceResponse } from "~/types/invoice";

function formatCurrency(value: number) {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value);
}

function formatDate(value?: string) {
    if (!value) return "-";
    return new Intl.DateTimeFormat("vi-VN").format(new Date(value));
}

function remainingAmount(invoice: InvoiceResponse) {
    return Math.max(0, invoice.totalAmountVnd - invoice.paidAmountVnd);
}

function statusBadge(invoice: InvoiceResponse) {
    switch (invoice.statusCode) {
        case "PAID":
            return <Tag tone="green">Đã thanh toán</Tag>;
        case "PARTIALLY_PAID":
            return <Tag tone="blue">Thanh toán một phần</Tag>;
        case "CANCELLED":
            return <Tag tone="red">Đã hủy</Tag>;
        case "REFUNDED":
            return <Tag tone="red">Đã hoàn tiền</Tag>;
        default:
            return <Tag tone="amber">Chờ thanh toán</Tag>;
    }
}

export function PaymentsPage() {
    const queryClient = useQueryClient();
    const [selectedInvoice, setSelectedInvoice] = useState<InvoiceResponse | null>(null);
    const [methodCode, setMethodCode] = useState<PaymentMethod>("BANK_TRANSFER");
    const [referenceCode, setReferenceCode] = useState("");
    const [note, setNote] = useState("");

    const invoicesQuery = useQuery({
        queryKey: ["my-invoices"],
        queryFn: () => invoiceApi.listMyInvoices({ page: 1, size: 50 }),
    });

    const paymentMutation = useMutation({
        mutationFn: () => {
            if (!selectedInvoice) throw new Error("Chưa chọn hóa đơn");
            return paymentApi.createOwnerPaymentRequest(selectedInvoice.id, {
                amountVnd: remainingAmount(selectedInvoice),
                methodCode,
                referenceCode: referenceCode.trim() || undefined,
                note: note.trim() || undefined,
            });
        },
        onSuccess: async () => {
            toast.success("Đã gửi xác nhận thanh toán, vui lòng chờ nhân viên duyệt");
            setSelectedInvoice(null);
            setReferenceCode("");
            setNote("");
            await queryClient.invalidateQueries({ queryKey: ["my-invoices"] });
        },
        onError: (error) => toast.error(error instanceof Error ? error.message : "Không thể gửi xác nhận thanh toán"),
    });

    const invoices = invoicesQuery.data?.content ?? [];
    const paidInvoices = invoices.filter((invoice) => invoice.statusCode === "PAID");
    const pendingInvoices = invoices.filter((invoice) => invoice.statusCode === "UNPAID" || invoice.statusCode === "PARTIALLY_PAID");
    const totalSpend = invoices.reduce((sum, invoice) => sum + (invoice.paidAmountVnd ?? 0), 0);

    return (
        <div className="space-y-6">
            <MiniGridStats
                items={[
                    { label: "Đã thanh toán", value: String(paidInvoices.length), hint: "Hóa đơn đã hoàn tất", icon: CheckCircle2 },
                    { label: "Chờ thanh toán", value: String(pendingInvoices.length), hint: "Có thể gửi xác nhận thanh toán", icon: Clock3 },
                    { label: "Tổng chi tiêu", value: formatCurrency(totalSpend), hint: "Theo dữ liệu hóa đơn", icon: CreditCard },
                    { label: "Dịch vụ", value: invoices.length ? "PCCMS" : "-", hint: "Từ các dòng hóa đơn", icon: Scissors },
                ]}
            />

            <Card title="Hóa đơn và thanh toán">
                {invoicesQuery.isLoading ? (
                    <div className="p-6 text-sm text-slate-500">Đang tải hóa đơn...</div>
                ) : invoicesQuery.isError ? (
                    <EmptyState title="Không thể tải hóa đơn" description="Vui lòng thử lại sau." />
                ) : invoices.length === 0 ? (
                    <EmptyState title="Chưa có hóa đơn" description="Hóa đơn của bạn sẽ xuất hiện tại đây." />
                ) : (
                    <DataTable
                        columns={["Mã HĐ", "Ngày", "Dịch vụ", "Tổng tiền", "Còn phải trả", "Trạng thái", "Hành động"]}
                        rows={invoices.map((invoice) => {
                            const remaining = remainingAmount(invoice);
                            return [
                                invoice.invoiceCode,
                                formatDate(invoice.issuedAt),
                                invoice.lines?.[0]?.description ?? invoice.note ?? "-",
                                formatCurrency(invoice.totalAmountVnd),
                                formatCurrency(remaining),
                                statusBadge(invoice),
                                remaining > 0 && invoice.statusCode !== "CANCELLED" ? (
                                    <Button key={invoice.id} variant="outline" className="h-auto px-3 py-1.5 text-xs" onClick={() => setSelectedInvoice(invoice)}>
                                        Xác nhận thanh toán
                                    </Button>
                                ) : (
                                    <span key={invoice.id} className="text-xs text-slate-500">Không cần thao tác</span>
                                ),
                            ];
                        })}
                    />
                )}
            </Card>

            <Modal isOpen={selectedInvoice !== null} onClose={() => setSelectedInvoice(null)} title="Xác nhận thanh toán">
                {selectedInvoice && (
                    <div className="space-y-4">
                        <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm">
                            <div className="font-medium text-slate-900">{selectedInvoice.invoiceCode}</div>
                            <div className="text-slate-600">Số tiền cần thanh toán: {formatCurrency(remainingAmount(selectedInvoice))}</div>
                        </div>
                        <div className="flex flex-col gap-1.5">
                            <label className="text-[13px] font-medium text-slate-700">Phương thức</label>
                            <select className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm" value={methodCode} onChange={(event) => setMethodCode(event.target.value as PaymentMethod)}>
                                <option value="BANK_TRANSFER">Chuyển khoản</option>
                                <option value="E_WALLET">Ví điện tử</option>
                                <option value="CARD">Thẻ</option>
                                <option value="CASH">Tiền mặt tại quầy</option>
                            </select>
                        </div>
                        <Input label="Mã giao dịch / nội dung chuyển khoản" value={referenceCode} onChange={(event) => setReferenceCode(event.target.value)} />
                        <Input label="Ghi chú" value={note} onChange={(event) => setNote(event.target.value)} />
                        <div className="flex justify-end gap-3">
                            <Button variant="outline" onClick={() => setSelectedInvoice(null)} disabled={paymentMutation.isPending}>Hủy</Button>
                            <Button onClick={() => paymentMutation.mutate()} disabled={paymentMutation.isPending || remainingAmount(selectedInvoice) <= 0}>
                                {paymentMutation.isPending ? "Đang xử lý..." : "Thanh toán"}
                            </Button>
                        </div>
                    </div>
                )}
            </Modal>
        </div>
    );
}
