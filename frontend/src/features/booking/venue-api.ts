import { api } from "@/lib/api"
import { buildPageQuery, type PageResponse } from "@/lib/pagination"

export type Shop = {
  id: string
  name: string
  address: string | null
  description: string | null
  status: string
  createdAt: string
}

export type TimeSlot = {
  id: string
  shopId: string
  resourceId: string
  startTime: string
  endTime: string
  price: number
  status: string
}

export type Resource = {
  id: string
  shopId: string
  name: string
  type: string
  createdAt: string
}

export function listShops() {
  return api<Shop[]>("/api/venue/shops", { auth: false })
}

export function listAdminShops(params: {
  page: number
  size?: number
  q?: string
  status?: string
}) {
  return api<PageResponse<Shop>>(`/api/venue/admin/shops?${buildPageQuery(params)}`)
}

export function listShopSlots(shopId: string, date: string) {
  return api<TimeSlot[]>(`/api/venue/shops/${shopId}/slots?date=${date}`, {
    auth: false,
  })
}

export function createResource(shopId: string, payload: { name: string; type: string }) {
  return api<Resource>(`/api/venue/shops/${shopId}/resources`, {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export function createSlot(
  shopId: string,
  payload: { resourceId: string; startTime: string; endTime: string; price: number }
) {
  return api<TimeSlot>(`/api/venue/shops/${shopId}/slots`, {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export function createShop(payload: {
  name: string
  address?: string
  description?: string
}) {
  return api<Shop>("/api/venue/admin/shops", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export function listShopResources(shopId: string) {
  return api<Resource[]>(`/api/venue/shops/${shopId}/resources`)
}
