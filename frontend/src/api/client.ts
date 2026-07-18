import axios, { type AxiosRequestConfig } from 'axios'
import type { ApiEnvelope, Page, PageMeta } from '@/types/api'
import type { TokenResponse } from '@/types/auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const REFRESH_TOKEN_KEY = 'agrodairy.refreshToken'

let accessToken: string | null = null
let onSessionExpired: (() => void) | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

export function getStoredRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setStoredRefreshToken(token: string | null) {
  if (token) {
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
  } else {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }
}

export function setSessionExpiredHandler(handler: (() => void) | null) {
  onSessionExpired = handler
}

export const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
})

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

const AUTH_ENDPOINT_PATTERN = /\/auth\/(login|register|refresh)$/

let refreshInFlight: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getStoredRefreshToken()
  if (!refreshToken) return null
  try {
    const response = await axios.post<ApiEnvelope<TokenResponse>>(
      `${API_BASE_URL}/api/auth/refresh`,
      { refreshToken },
    )
    const tokens = response.data.data
    setAccessToken(tokens.accessToken)
    setStoredRefreshToken(tokens.refreshToken)
    return tokens.accessToken
  } catch {
    return null
  }
}

interface RetryableConfig extends AxiosRequestConfig {
  _retried?: boolean
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as RetryableConfig | undefined
    const isAuthEndpoint = AUTH_ENDPOINT_PATTERN.test(originalRequest?.url ?? '')

    if (error.response?.status === 401 && originalRequest && !originalRequest._retried && !isAuthEndpoint) {
      originalRequest._retried = true
      if (!refreshInFlight) {
        refreshInFlight = refreshAccessToken().finally(() => {
          refreshInFlight = null
        })
      }
      const newToken = await refreshInFlight
      if (newToken) {
        originalRequest.headers = { ...originalRequest.headers, Authorization: `Bearer ${newToken}` }
        return api(originalRequest)
      }
      setAccessToken(null)
      setStoredRefreshToken(null)
      onSessionExpired?.()
    }
    return Promise.reject(error)
  },
)

export async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>): Promise<T> {
  const response = await promise
  return response.data.data
}

export async function unwrapPage<T>(promise: Promise<{ data: ApiEnvelope<T[]> }>): Promise<Page<T>> {
  const response = await promise
  const meta: PageMeta = response.data.meta ?? { page: 0, size: response.data.data.length, totalElements: response.data.data.length, totalPages: 1 }
  return { items: response.data.data, meta }
}

export function apiErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as { error?: { message?: string } } | undefined
    return body?.error?.message ?? fallback
  }
  return fallback
}
