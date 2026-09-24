export type Role = "ADMIN" | "SHOP_MANAGER" | "USER"

export type User = {
  id: string
  email: string
  fullName: string
  phone: string | null
  role: Role
  status: string
  shopIds: string[]
  createdAt: string
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInMs: number
  user: User
}

const ACCESS_KEY = "bp.accessToken"
const REFRESH_KEY = "bp.refreshToken"
const USER_KEY = "bp.user"

export function loadSession(): { accessToken: string; refreshToken: string; user: User } | null {
  const accessToken = sessionStorage.getItem(ACCESS_KEY)
  const refreshToken = sessionStorage.getItem(REFRESH_KEY)
  const rawUser = sessionStorage.getItem(USER_KEY)
  if (!accessToken || !refreshToken || !rawUser) return null
  try {
    return { accessToken, refreshToken, user: JSON.parse(rawUser) as User }
  } catch {
    return null
  }
}

export function saveSession(auth: AuthResponse) {
  sessionStorage.setItem(ACCESS_KEY, auth.accessToken)
  sessionStorage.setItem(REFRESH_KEY, auth.refreshToken)
  sessionStorage.setItem(USER_KEY, JSON.stringify(auth.user))
}

export function clearSession() {
  sessionStorage.removeItem(ACCESS_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export function getAccessToken() {
  return sessionStorage.getItem(ACCESS_KEY)
}

export function homePathForRole(role: Role) {
  switch (role) {
    case "ADMIN":
      return "/admin"
    case "SHOP_MANAGER":
      return "/manager"
    default:
      return "/app"
  }
}
