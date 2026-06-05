export type UserStatus = 'ACTIVE' | 'LOCKED' | 'DISABLED' | 'UNVERIFIED';
export type UserRole = 'ADMIN' | 'VETERINARIAN' | 'STAFF' | 'OWNER';

export interface UserResponse {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  roleCode: UserRole;
  createdAt: string;
  statusCode: UserStatus;
}

export interface CreateUserRequest {
  email: string;
  fullName: string;
  phone?: string;
  roleCode: UserRole;
  password?: string;
}

export interface UpdateUserRequest {
  fullName: string;
  phone?: string;
}
