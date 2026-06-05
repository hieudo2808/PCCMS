export type GroomingStatus = "PENDING" | "CONFIRMED" | "IN_SERVICE" | "COMPLETED" | "CANCELLED";
export type AppointmentStatus = "PENDING" | "CONFIRMED" | "CHECKED_IN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type ServiceOrderStatus = "REQUESTED" | "CONFIRMED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type InvoiceStatus = "DRAFT" | "UNPAID" | "PARTIALLY_PAID" | "PAID" | "OVERDUE" | "CANCELLED" | "REFUNDED";

export interface GroomingInvoiceSummaryResponse {
  id: string;
  invoiceCode: string;
  statusCode: InvoiceStatus;
  totalAmountVnd: number;
  paidAmountVnd: number;
  issuedAt: string;
}

export interface GroomingServiceResponse {
  id: string;
  serviceCode: string;
  name: string;
  description?: string;
  basePriceVnd: number;
  durationMinutes: number;
  isActive: boolean;
}

export interface GroomingStationResponse {
  id: string;
  stationCode: string;
  name: string;
  isActive: boolean;
}

export interface GroomingTicketResponse {
  id: string;
  appointmentId: string;
  serviceOrderId: string;
  orderCode: string;
  serviceOrderStatus: ServiceOrderStatus;
  ownerId: string;
  ownerName: string;
  petId: string;
  petName: string;
  serviceId: string;
  serviceCode: string;
  serviceName: string;
  basePriceVnd: number;
  durationMinutes: number;
  scheduledStartAt: string;
  scheduledEndAt: string;
  appointmentStatus: AppointmentStatus;
  statusCode: GroomingStatus;
  stationId?: string;
  stationCode?: string;
  stationName?: string;
  assignedStaffId?: string;
  assignedStaffName?: string;
  startedAt?: string;
  completedAt?: string;
  ownerNote?: string;
  internalNote?: string;
  estimatedAmountVnd: number;
  finalAmountVnd?: number;
  invoice?: GroomingInvoiceSummaryResponse;
}

export interface CreateGroomingBookingRequest {
  petId: string;
  serviceId: string;
  scheduledStartAt: string;
  ownerNote?: string;
}

export interface GroomingServiceRequest {
  serviceCode: string;
  name: string;
  description?: string;
  basePriceVnd: number;
  durationMinutes: number;
}

export interface GroomingStationRequest {
  stationCode: string;
  name: string;
  isActive: boolean;
}
