export type ServiceCategory = 'MEDICAL' | 'GROOMING' | 'BOARDING' | 'OTHER';

export interface ServiceCatalogResponse {
  id: string;
  serviceCode: string;
  name: string;
  categoryCode: ServiceCategory;
  categoryLabel: string;
  description?: string;
  basePriceVnd: number;
  durationMinutes?: number;
  isActive: boolean;
}

export interface CreateServiceCatalogRequest {
  serviceCode: string;
  name: string;
  categoryCode: ServiceCategory;
  description?: string;
  basePriceVnd: number;
  durationMinutes?: number;
  isActive: boolean;
}

export type UpdateServiceCatalogRequest = CreateServiceCatalogRequest;

export type RoomStatus = 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'INACTIVE';

export interface RoomResponse {
  id: string;
  roomCode: string;
  name: string;
  roomTypeId: string;
  roomTypeName: string;
  floor: number;
  capacity: number;
  statusCode: RoomStatus;
  statusLabel: string;
  description?: string;
}

export interface CreateRoomRequest {
  roomCode: string;
  name: string;
  roomTypeId: string;
  capacity: number;
  statusCode: RoomStatus;
  floor?: number;
  description?: string;
}

export type UpdateRoomRequest = CreateRoomRequest;

export interface RoomTypeResponse {
  id: string;
  code: string;
  name: string;
  defaultCapacity: number;
  baseDailyPriceVnd: number;
  description?: string;
  isActive: boolean;
}

export interface MedicineCategoryResponse {
  id: string;
  name: string;
  description?: string;
  isActive: boolean;
}

export interface CreateMedicineCategoryRequest {
  name: string;
  description?: string;
  isActive: boolean;
}

export type UpdateMedicineCategoryRequest = CreateMedicineCategoryRequest;

export interface CreateRoomTypeRequest {
  code: string;
  name: string;
  defaultCapacity: number;
  baseDailyPriceVnd: number;
  description?: string;
  isActive: boolean;
}

export type UpdateRoomTypeRequest = CreateRoomTypeRequest;
