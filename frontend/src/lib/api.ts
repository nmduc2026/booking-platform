import { getAccessToken } from "@/lib/auth-storage"

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

export class ApiError extends Error {
  readonly status: number
  readonly body: string

  constructor(status: number, body: string) {
    super(body || `Request failed with status ${status}`)
    this.name = "ApiError"
    this.status = status
    this.body = body
  }
}

type ApiOptions = RequestInit & {
  auth?: boolean
}

export async function api<T>(path: string, init: ApiOptions = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json")
  }

  if (init.auth !== false) {
    const token = getAccessToken()
    if (token) {
      headers.set("Authorization", `Bearer ${token}`)
    }
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
  })

  if (!response.ok) {
    throw new ApiError(response.status, await response.text())
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  if (!text) {
    return undefined as T
  }

  return JSON.parse(text) as T
}
