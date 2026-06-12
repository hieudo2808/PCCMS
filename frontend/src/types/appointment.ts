import type { AppointmentStatus } from "./grooming";
export type { AppointmentStatus };
export type { BoardingBookingResponse } from "./boarding";

export interface AvailabilitySummaryResponse { [key: string]: any; }
export interface CreateMedicalAppointmentRequest { [key: string]: any; }
export interface GroomingBoardCardResponse { [key: string]: any; }
export interface QueueEntryResponse { [key: string]: any; }
export interface QuickCheckInRequest { [key: string]: any; }
export interface RoomTypeOptionResponse { [key: string]: any; }
export interface ServiceCatalogOptionResponse { [key: string]: any; }
export interface TimeSlotResponse { [key: string]: any; }
export interface VetOptionResponse { [key: string]: any; }

export type AppointmentType = "MEDICAL" | "GROOMING";

export interface AppointmentResponse {
    id: string;
    appointmentCode: string;
    serviceOrderId: string;
    appointmentType: AppointmentType;
    scheduledStartAt: string;
    scheduledEndAt: string;
    petId?: string;
    petName?: string;
    ownerId?: string;
    ownerName?: string;
    ownerPhone?: string;
    requestedStaffId?: string;
    assignedStaffId?: string;
    assignedVetName?: string;
    statusCode: AppointmentStatus;
    statusLabel?: string;
    examRoomId?: string;
    symptomText?: string;
    ownerNote?: string;
    internalNote?: string;
}
