// ─── Mock Data – Cashier Page ─────────────────────────────────────────────────
// TODO: Replace with API call → GET /api/bills
// when backend is ready.

export type BillStatus = "Chờ thanh toán" | "Đã thanh toán";
export type ServiceType = "Spa" | "Lưu trú" | "Khám bệnh";
export type PayMethod = "Tiền mặt" | "Chuyển khoản" | "Thẻ ngân hàng";

export interface BillItem {
    desc: string;
    qty: number;
    unitPrice: number;
}

export interface Bill {
    id: string;
    code: string;
    ownerName: string;
    ownerPhone: string;
    petName: string;
    serviceType: ServiceType;
    items: BillItem[];
    status: BillStatus;
    createdAt: string;
    paidAt?: string;
    payMethod?: PayMethod;
    note?: string;
}

export const SERVICE_TAG_TONE: Record<ServiceType, "blue" | "amber" | "green"> = {
    Spa: "blue",
    "Lưu trú": "amber",
    "Khám bệnh": "green",
};

export const SERVICE_COLOR: Record<ServiceType, string> = {
    Spa: "bg-blue-100 text-blue-600",
    "Lưu trú": "bg-amber-100 text-amber-600",
    "Khám bệnh": "bg-emerald-100 text-emerald-600",
};

// FIX #1 & #5: Đồng bộ tổng 840k với PaymentsPage; khớp mã HD với PaymentsPage
export const MOCK_BILLS: Bill[] = [
    {
        id: "1",
        code: "HD-2026-041",
        ownerName: "Nguyễn Văn A",
        ownerPhone: "0912 345 678",
        petName: "Milu (Poodle)",
        serviceType: "Lưu trú",
        items: [
            { desc: "Chuồng Deluxe (3 đêm)", qty: 3, unitPrice: 280_000 },
        ],
        status: "Chờ thanh toán",
        createdAt: "01/06/2026 - 03:15",
        note: "Bé sợ tiếng ồn",
    },
    {
        id: "2",
        code: "HD-2026-038",
        ownerName: "Nguyễn Văn A",
        ownerPhone: "0912 345 678",
        petName: "Milu (Poodle)",
        serviceType: "Spa",
        items: [
            { desc: "Tắm + Sấy + Cắt tỉa", qty: 1, unitPrice: 250_000 },
        ],
        status: "Chờ thanh toán",
        createdAt: "24/05/2026 - 08:30",
    },
    {
        id: "3",
        code: "HD-2026-039",
        ownerName: "Phạm Văn C",
        ownerPhone: "0901 111 222",
        petName: "Mít (Mèo ALN)",
        serviceType: "Khám bệnh",
        items: [
            { desc: "Khám định kỳ", qty: 1, unitPrice: 200_000 },
            { desc: "Siêu âm bụng", qty: 1, unitPrice: 150_000 },
            { desc: "Thuốc tiêu hóa", qty: 2, unitPrice: 35_000 },
        ],
        status: "Chờ thanh toán",
        createdAt: "02/05/2026 - 09:00",
    },
    {
        id: "4",
        code: "HD-2026-032",
        ownerName: "Phạm Văn C",
        ownerPhone: "0901 111 222",
        petName: "Mít (Mèo ALN)",
        serviceType: "Khám bệnh",
        items: [{ desc: "Khám định kỳ + kê đơn", qty: 1, unitPrice: 420_000 }],
        status: "Đã thanh toán",
        createdAt: "20/05/2026 - 10:00",
        paidAt: "20/05/2026 - 10:45",
        payMethod: "Chuyển khoản",
    },
    {
        id: "5",
        code: "HD-2026-021",
        ownerName: "Trần Thị E",
        ownerPhone: "0977 111 333",
        petName: "Bơ (Corgi)",
        serviceType: "Spa",
        items: [{ desc: "Spa Premium (full option)", qty: 1, unitPrice: 450_000 }],
        status: "Đã thanh toán",
        createdAt: "14/05/2026 - 14:00",
        paidAt: "14/05/2026 - 15:00",
        payMethod: "Tiền mặt",
    },
];
