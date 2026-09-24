import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react"

import { me } from "@/features/auth/api"
import {
  clearSession,
  homePathForRole,
  loadSession,
  saveSession,
  type AuthResponse,
  type User,
} from "@/lib/auth-storage"

type AuthContextValue = {
  user: User | null
  accessToken: string | null
  isAuthenticated: boolean
  setAuth: (auth: AuthResponse) => void
  logout: () => void
  refreshMe: () => Promise<User | null>
  homePath: string
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const initial = loadSession()
  const [user, setUser] = useState<User | null>(initial?.user ?? null)
  const [accessToken, setAccessToken] = useState<string | null>(
    initial?.accessToken ?? null
  )

  const setAuth = useCallback((auth: AuthResponse) => {
    saveSession(auth)
    setUser(auth.user)
    setAccessToken(auth.accessToken)
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setUser(null)
    setAccessToken(null)
  }, [])

  const refreshMe = useCallback(async () => {
    if (!accessToken) return null
    const profile = await me()
    setUser(profile)
    const session = loadSession()
    if (session) {
      saveSession({ ...session, user: profile, accessToken: session.accessToken, refreshToken: session.refreshToken, tokenType: "Bearer", expiresInMs: 0 })
    }
    return profile
  }, [accessToken])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      accessToken,
      isAuthenticated: Boolean(user && accessToken),
      setAuth,
      logout,
      refreshMe,
      homePath: user ? homePathForRole(user.role) : "/login",
    }),
    [user, accessToken, setAuth, logout, refreshMe]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider")
  }
  return ctx
}
