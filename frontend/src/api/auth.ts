import { api, unwrap } from '@/api/client'
import type { AuthResponse, CreateStaffRequest, LoginRequest, RegisterRequest, TokenResponse, User } from '@/types/auth'

export function login(request: LoginRequest) {
  return unwrap<AuthResponse>(api.post('/auth/login', request))
}

export function register(request: RegisterRequest) {
  return unwrap<AuthResponse>(api.post('/auth/register', request))
}

export function google(idToken: string) {
  return unwrap<AuthResponse>(api.post('/auth/google', { idToken }))
}

export function refresh(refreshToken: string) {
  return unwrap<TokenResponse>(api.post('/auth/refresh', { refreshToken }))
}

export function logout(refreshToken: string) {
  return api.post('/auth/logout', { refreshToken })
}

export function me() {
  return unwrap<User>(api.get('/auth/me'))
}

export function createStaff(request: CreateStaffRequest) {
  return unwrap<{ user: User }>(api.post('/auth/staff', request))
}
