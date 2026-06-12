export type BoardingStatus = "RESERVED" | "CHECKED_IN" | "IN_STAY" | "CHECKED_OUT" | "CANCELLED";
export type ServiceOrderStatus =
    | "REQUESTED"
    | "CONFIRMED"
    | "IN_PROGRESS"
    | "COMPLETED"
    | "CANCELLED";
export type CarePeriod = "MORNING" | "NOON" | "AFTERNOON";
export type RoomStatus = "AVAILABLE" | "OCCUPIED" | "MAINTENANCE" | "INACTIVE";
export type InvoiceStatus =
    | "DRAFT"
    | "UNPAID"
    | "PARTIALLY_PAID"
    | "PAID"
    | "OVERDUE"
    | "CANCELLED"
    | "REFUNDED";

export interface BoardingStay { [key: string]: any; }
export interface CareLogEntry { [key: string]: any; }
export interface StaffBoardingStay { [key: string]: any; }
export interface UpsertCareLogPayload { [key: string]: any; }

export interface RoomTypeResponse {
    id: string;
    code: string;
    name: string;
    defaultCapacity: number;
    baseDailyPriceVnd: number;
    description?: string;
    isActive: boolean;
}

export interface RoomResponse {
    id: string;
    roomCode: string;
    name: string;
    roomTypeId: string;
    roomTypeName: string;
    floor: number;
    capacity: number;
    statusCode: RoomStatus;
    description?: string;
}

export interface RoomAvailabilityResponse {
    roomTypeId: string;
    roomTypeCode: string;
    roomTypeName: string;
    defaultCapacity: number;
    baseDailyPriceVnd: number;
    availableRooms: number;
}

export interface InvoiceSummaryResponse {
    id: string;
    invoiceCode: string;
    statusCode: InvoiceStatus;
    totalAmountVnd: number;
    paidAmountVnd: number;
    issuedAt: string;
}

export interface BoardingBookingResponse {
    id: string;
    bookingCode: string;
    sessionId?: string;
    serviceOrderId: string;
    orderCode: string;
    serviceOrderStatus: ServiceOrderStatus;
    ownerId: string;
    ownerName: string;
    petId: string;
    petName: string;
    requestedRoomTypeId: string;
    requestedRoomTypeName: string;
    roomId?: string;
    roomCode?: string;
    roomName?: string;
    roomTypeName?: string;
    expectedCheckinAt: string;
    expectedCheckoutAt: string;
    actualCheckinAt?: string;
    actualCheckoutAt?: string;
    specialCareRequest?: string;
    estimatedPriceVnd: number;
    finalAmountVnd?: number;
    statusCode: BoardingStatus;
    statusLabel?: string;
    invoice?: InvoiceSummaryResponse;
}

export interface CareLogMediaResponse {
    id: string;
    fileId: string;
    url: string;
    mimeType: string;
    caption?: string;
}

export interface CareLogResponse {
    id: string;
    sessionId: string;
    logDate: string;
    periodCode: CarePeriod;
    feedingStatus: string;
    hygieneStatus: string;
    healthNote?: string;
    staffNote?: string;
    staffId: string;
    staffName: string;
    createdAt: string;
    media: CareLogMediaResponse[];
}

export interface CreateBoardingBookingRequest {
    petId: string;
    roomTypeId: string;
    expectedCheckinAt: string;
    expectedCheckoutAt: string;
    specialCareRequest?: string;
}

export interface RoomTypeRequest {
    code: string;
    name: string;
    defaultCapacity: number;
    baseDailyPriceVnd: number;
    description?: string;
}

export interface RoomRequest {
    roomCode: string;
    name: string;
    roomTypeId: string;
    floor: number;
    capacity: number;
    statusCode: RoomStatus;
    description?: string;
}
