import { api } from "@/lib/api"

export type Booking = {
  id: string
  userId: string
  shopId: string
  slotId: string
  status: string
  amount: number
  expiresAt: string
  createdAt: string
}

export function createBooking(slotId: string) {
  return api<Booking>("/api/booking/bookings", {
    method: "POST",
    body: JSON.stringify({ slotId }),
  })
}

export function listMyBookings() {
  return api<Booking[]>("/api/booking/bookings/me")
}

export function getBooking(bookingId: string) {
  return api<Booking>(`/api/booking/bookings/${bookingId}`)
}

export function listShopBookings(shopId: string) {
  return api<Booking[]>(`/api/booking/bookings/shop/${shopId}`)
}
