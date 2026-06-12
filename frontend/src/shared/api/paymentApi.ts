import axiosClient from "~/shared/api/axiosClient";

export type PaymentMethod = "CASH" | "BANK_TRANSFER" | "CARD" | "E_WALLET";

export interface OwnerPaymentRequest {
    amountVnd: number;
    methodCode: PaymentMethod;
    referenceCode?: string;
    note?: string;
    proofFileId?: string;
}

export interface PaymentResponse {
    id: string;
    paymentCode: string;
    invoiceId: string;
    amountVnd: number;
    methodCode: PaymentMethod;
    statusCode: "PENDING" | "SUCCEEDED" | "FAILED" | "CANCELLED" | "REFUNDED";
    paidAt?: string;
    receivedBy?: string;
    note?: string;
}

export const paymentApi = {
    createOwnerPaymentRequest: (invoiceId: string, data: OwnerPaymentRequest): Promise<PaymentResponse> =>
        axiosClient.post(`/v1/me/invoices/${invoiceId}/payment-requests`, data),
};
