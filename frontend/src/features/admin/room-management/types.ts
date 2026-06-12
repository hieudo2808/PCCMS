export type RoomType = "Thường" | "VIP" | "Cách ly";
export type RoomStatus = "Trống" | "Đang sử dụng" | "Bảo trì" | "Ngừng hoạt động";

export interface BoardingRoom {
    id: string;
    code: string;
    name: string;
    roomTypeId?: string;
    type: RoomType;
    floor?: number;
    capacity: number;
    status: RoomStatus;
    note: string;
    isReferenced?: boolean;
}

export interface RoomSearchParams {
    keyword: string;
    type: RoomType | "";
    roomTypeId: string;
    status: RoomStatus | "";
    floor: string;
    capacity: string;
}

export interface RoomFormValues {
    code: string;
    name: string;
    type: RoomType | "";
    roomTypeId: string;
    floor: string;
    capacity: string;
    status: RoomStatus | "";
    note: string;
}
