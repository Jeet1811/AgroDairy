import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '@/api/auth'
import { getStoredRefreshToken, setAccessToken, setSessionExpiredHandler, setStoredRefreshToken } from '@/api/client'
import type { LoginRequest, RegisterRequest, User } from '@/types/auth'

interface AuthContextValue {
  user: User | null
  isLoading: boolean
  login: (request: LoginRequest) => Promise<User>
  register: (request: RegisterRequest) => Promise<User>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const clearSession = useCallback(() => {
    setAccessToken(null)
    setStoredRefreshToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    setSessionExpiredHandler(clearSession)
    return () => setSessionExpiredHandler(null)
  }, [clearSession])

  useEffect(() => {
    const storedRefreshToken = getStoredRefreshToken()
    if (!storedRefreshToken) {
      setIsLoading(false)
      return
    }
    authApi
      .refresh(storedRefreshToken)
      .then(async (tokens) => {
        setAccessToken(tokens.accessToken)
        setStoredRefreshToken(tokens.refreshToken)
        const currentUser = await authApi.me()
        setUser(currentUser)
      })
      .catch(clearSession)
      .finally(() => setIsLoading(false))
  }, [clearSession])

  const login = useCallback(async (request: LoginRequest) => {
    const response = await authApi.login(request)
    setAccessToken(response.accessToken)
    setStoredRefreshToken(response.refreshToken)
    setUser(response.user)
    return response.user
  }, [])

  const register = useCallback(async (request: RegisterRequest) => {
    const response = await authApi.register(request)
    setAccessToken(response.accessToken)
    setStoredRefreshToken(response.refreshToken)
    setUser(response.user)
    return response.user
  }, [])

  const logout = useCallback(async () => {
    const storedRefreshToken = getStoredRefreshToken()
    clearSession()
    if (storedRefreshToken) {
      try {
        await authApi.logout(storedRefreshToken)
      } catch {
        // best-effort — session is already cleared client-side
      }
    }
  }, [clearSession])

  const value = useMemo<AuthContextValue>(
    () => ({ user, isLoading, login, register, logout }),
    [user, isLoading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
