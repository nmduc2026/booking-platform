import { api } from "@/lib/api"
import type { AuthResponse, User } from "@/lib/auth-storage"
import { buildPageQuery, type PageResponse } from "@/lib/pagination"

export function login(email: string, password: string) {
  return api<AuthResponse>("/api/auth/login", {
    method: "POST",
    auth: false,
    body: JSON.stringify({ email, password }),
  })
}

export function register(payload: {
  email: string
  password: string
  fullName: string
  phone?: string
}) {
  return api<AuthResponse>("/api/auth/register", {
    method: "POST",
    auth: false,
    body: JSON.stringify(payload),
  })
}

export function me() {
  return api<User>("/api/auth/me")
}

export function listAdminUsers(params: {
  page: number
  size?: number
  q?: string
  role?: string
}) {
  return api<PageResponse<User>>(`/api/auth/admin/users?${buildPageQuery(params)}`)
}

export function createAdminUser(payload: {
  email: string
  password: string
  fullName: string
  phone?: string
  role: string
}) {
  return api<User>("/api/auth/admin/users", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export function mapUserToShop(payload: { userId: string; shopId: string }) {
  return api<{ id: string; userId: string; shopId: string }>(
    "/api/auth/admin/user-shop-mapping",
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  )
}
