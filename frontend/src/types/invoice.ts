import type { InvoiceStatus } from "./boarding";

export interface InvoiceLineResponse {
    id: string;
    invoiceId: string;
    serviceOrderId?: string;
    medicineId?: string;
    description: string;
    quantity: number;
    unitPriceVnd: number;
    totalAmountVnd: number;
}

export interface InvoiceResponse {
    id: string;
    invoiceCode: string;
    ownerId: string;
    petId: string;
    statusCode: InvoiceStatus;
    totalAmountVnd: number;
    paidAmountVnd: number;
    issuedAt: string;
    note?: string;
    lines: InvoiceLineResponse[];
}
