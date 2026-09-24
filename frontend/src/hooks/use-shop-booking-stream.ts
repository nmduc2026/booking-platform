import { useEffect, useRef } from "react"

import { getAccessToken } from "@/lib/auth-storage"

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

export type BookingSseEvent = {
  eventType: string
  bookingId: string
  shopId: string
  slotId: string
  status: string
  amount?: string
}

export function useShopBookingStream(
  shopId: string | null | undefined,
  onEvent: (event: BookingSseEvent) => void
) {
  const handlerRef = useRef(onEvent)
  handlerRef.current = onEvent

  useEffect(() => {
    if (!shopId) return
    const token = getAccessToken()
    if (!token) return

    const url = `${API_BASE}/api/realtime/realtime/shops/${shopId}/stream?access_token=${encodeURIComponent(token)}`
    const source = new EventSource(url)

    const listener = (event: MessageEvent) => {
      try {
        handlerRef.current(JSON.parse(event.data) as BookingSseEvent)
      } catch {
        // ignore malformed payloads
      }
    }

    source.addEventListener("booking", listener)
    return () => {
      source.removeEventListener("booking", listener)
      source.close()
    }
  }, [shopId])
}
