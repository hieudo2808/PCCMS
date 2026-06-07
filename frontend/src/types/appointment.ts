import type { AppointmentStatus } from "./grooming";

export type AppointmentType = "MEDICAL" | "GROOMING";

export interface AppointmentResponse {
    id: string;
    serviceOrderId: string;
    appointmentType: AppointmentType;
    scheduledStartAt: string;
    scheduledEndAt: string;
    requestedStaffId?: string;
    assignedStaffId?: string;
    statusCode: AppointmentStatus;
    examRoomId?: string;
    symptomText?: string;
    ownerNote?: string;
    internalNote?: string;
}
