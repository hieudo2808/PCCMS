export type AppointmentStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export type AppointmentType = 'MEDICAL' | 'GROOMING' | 'OTHER';

export interface AppointmentResponse {
  id: string;
  appointmentCode: string;
  appointmentType?: AppointmentType;
  serviceName?: string;
  scheduledStartAt: string;
  scheduledEndAt: string;
  ownerName: string;
  ownerPhone: string | null;
  petId: string;
  petName: string;
  assignedVetId: string | null;
  assignedVetName: string | null;
  statusCode: AppointmentStatus;
  statusLabel: string;
  symptomText: string | null;
  ownerNote: string | null;
  queueNumber: number | null;
}

export interface GroomingBoardCardResponse {
  ticketId: string;
  appointmentId: string;
  petName: string;
  serviceName: string;
  scheduledStartAt: string;
  statusCode: string;
  statusLabel: string;
  stationName: string | null;
}

export interface BoardingBookingResponse {
  id: string;
  bookingCode: string;
  petId: string;
  petName: string;
  roomTypeName: string;
  expectedCheckinAt: string;
  expectedCheckoutAt: string;
  estimatedPriceVnd: number;
  statusCode: string;
  statusLabel: string;
  specialCareRequest: string | null;
}

export interface RoomTypeOptionResponse {
  id: string;
  code: string;
  name: string;
  baseDailyPriceVnd: number;
}

export interface ServiceCatalogOptionResponse {
  id: string;
  serviceCode: string;
  name: string;
  categoryCode: string;
  basePriceVnd: number;
  durationMinutes: number | null;
}

export interface TimeSlotResponse {
  startTime: string;
  endTime: string;
  label: string;
  available: boolean;
}

export interface VetOptionResponse {
  id: string;
  fullName: string;
  available: boolean;
}

export interface AvailabilitySummaryResponse {
  totalExamRooms: number;
  vetsOnDuty: number;
  totalSlots: number;
  availableSlots: number;
  freeRoomsForSlot: number | null;
  freeVetsForSlot: number | null;
}

export interface QueueEntryResponse {
  queueNumber: number;
  appointmentId: string;
  petId: string;
  petName: string;
  ownerName: string;
  checkedInAt: string;
  symptomText: string | null;
}

export interface CreateMedicalAppointmentRequest {
  petId: string;
  appointmentDate: string;
  slotStart: string;
  requestedVetId?: string;
  symptomText: string;
  ownerNote?: string;
}

export interface QuickCheckInRequest {
  phone: string;
  petId: string;
  assignedVetId?: string;
  symptomText?: string;
}
