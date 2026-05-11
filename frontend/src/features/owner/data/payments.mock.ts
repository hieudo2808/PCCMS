// ─── Mock Data – Payments Page (Owner) ───────────────────────────────────────
// TODO: Replace with API call → GET /api/payments/mine
// when backend is ready.

export type InvoiceStatus = "Chờ thanh toán" | "Đã thanh toán" | "Đã hủy";
export type ServiceType = "Spa" | "Lưu trú" | "Khám bệnh";
export type PayMethod = "Tiền mặt" | "Chuyển khoản" | "Thẻ ngân hàng";

export interface Invoice {
    id: string;
    code: string;
    date: string;
    serviceType: ServiceType;
    serviceDetail: string;
    petName: string;
    amount: number;
    status: InvoiceStatus;
    paidAt?: string;
    payMethod?: PayMethod;
}

export const SERVICE_TONE: Record<ServiceType, "blue" | "amber" | "green"> = {
    Spa: "blue",
    "Lưu trú": "amber",
    "Khám bệnh": "green",
};

export const STATUS_TONE: Record<InvoiceStatus, "amber" | "green" | "red"> = {
    "Chờ thanh toán": "amber",
    "Đã thanh toán": "green",
    "Đã hủy": "red",
};

export const PAY_METHODS: { method: PayMethod; desc: string }[] = [
    { method: "Tiền mặt", desc: "Thanh toán trực tiếp tại quầy lễ tân" },
    { method: "Chuyển khoản", desc: "VietQR · MB Bank · Vietcombank" },
    { method: "Thẻ ngân hàng", desc: "Visa, MasterCard, Napas" },
];

export const MOCK_INVOICES: Invoice[] = [
    {
        id: "1",
        code: "HD-2026-041",
        date: "01/06/2026",
        serviceType: "Lưu trú",
        serviceDetail: "Chuồng Deluxe · 3 đêm (01/06 → 04/06)",
        petName: "Milu (Poodle)",
        amount: 840_000,
        status: "Chờ thanh toán",
    },
    {
        id: "2",
        code: "HD-2026-038",
        date: "24/05/2026",
        serviceType: "Spa",
        serviceDetail: "Tắm + Sấy + Cắt tỉa",
        petName: "Milu (Poodle)",
        amount: 250_000,
        status: "Chờ thanh toán",
    },
    {
        id: "3",
        code: "HD-2026-032",
        date: "20/05/2026",
        serviceType: "Khám bệnh",
        serviceDetail: "Khám định kỳ + kê đơn",
        petName: "Mít (Mèo ALN)",
        amount: 420_000,
        status: "Đã thanh toán",
        paidAt: "20/05/2026 10:45",
        payMethod: "Chuyển khoản",
    },
    {
        id: "4",
        code: "HD-2026-021",
        date: "14/05/2026",
        serviceType: "Spa",
        serviceDetail: "Spa Premium (full option)",
        petName: "Bơ (Corgi)",
        amount: 450_000,
        status: "Đã thanh toán",
        paidAt: "14/05/2026 15:00",
        payMethod: "Tiền mặt",
    },
    {
        id: "5",
        code: "HD-2026-011",
        date: "05/05/2026",
        serviceType: "Lưu trú",
        serviceDetail: "Chuồng tiêu chuẩn · 2 đêm",
        petName: "Milu (Poodle)",
        amount: 300_000,
        status: "Đã thanh toán",
        paidAt: "07/05/2026 09:10",
        payMethod: "Thẻ ngân hàng",
    },
];
