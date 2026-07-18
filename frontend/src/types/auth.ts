export type Role = 'ADMIN' | 'STAFF' | 'CUSTOMER'

export interface User {
  id: string
  email: string
  fullName: string
  role: Role
  phone: string | null
  isActive: boolean
  createdAt: string
}

export interface AuthResponse {
  user: User
  accessToken: string
  refreshToken: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
  phone?: string
}

export interface CreateStaffRequest {
  email: string
  password: string
  fullName: string
  phone?: string
  role: Role
}
