import axiosClient from "~/shared/api/axiosClient";
import { normalizePage, toSpringPage } from "~/shared/api/pageUtils";
import type { PageResponse } from "~/types/api";
import type { InvoiceResponse } from "~/types/invoice";

export const invoiceApi = {
    listMyInvoices: async (params?: { page?: number; size?: number }): Promise<PageResponse<InvoiceResponse>> => {
        const raw = await axiosClient.get("/v1/invoices/my", {
            params: {
                page: params?.page != null ? toSpringPage(params.page) : undefined,
                size: params?.size,
            },
        });
        return normalizePage<InvoiceResponse>(raw);
    },

    getInvoice: (invoiceId: string): Promise<InvoiceResponse> => {
        return axiosClient.get(`/v1/invoices/${invoiceId}`);
    },
};
