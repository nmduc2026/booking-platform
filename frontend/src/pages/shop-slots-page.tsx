import { useQuery, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { Link, useNavigate, useParams } from "react-router"

import { AppShell } from "@/components/app-shell"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { createBooking } from "@/features/booking/booking-api"
import { listShopSlots } from "@/features/booking/venue-api"
import { useShopBookingStream } from "@/hooks/use-shop-booking-stream"
import { ApiError } from "@/lib/api"

const userNav = [
  { to: "/app", label: "Home" },
  { to: "/app/shops", label: "Find shops" },
  { to: "/app/bookings", label: "My bookings" },
]

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

export function ShopSlotsPage() {
  const { shopId = "" } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [date, setDate] = useState(todayIso())
  const [busySlotId, setBusySlotId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const slotsQuery = useQuery({
    queryKey: ["shop-slots", shopId, date],
    queryFn: () => listShopSlots(shopId, date),
    enabled: Boolean(shopId),
  })

  useShopBookingStream(shopId, () => {
    void queryClient.invalidateQueries({ queryKey: ["shop-slots", shopId, date] })
  })

  async function bookSlot(slotId: string) {
    setError(null)
    setBusySlotId(slotId)
    try {
      const booking = await createBooking(slotId)
      navigate(`/app/bookings/${booking.id}/checkout`)
    } catch (err) {
      setError(err instanceof ApiError ? err.body || err.message : "Booking failed")
    } finally {
      setBusySlotId(null)
    }
  }

  return (
    <AppShell title="Available slots" nav={userNav}>
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div>
          <label className="mb-1 block text-sm text-muted-foreground" htmlFor="date">
            Date
          </label>
          <Input
            id="date"
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-48"
          />
        </div>
        <Button variant="outline" nativeButton={false} render={<Link to="/app/shops" />}>
          Back to shops
        </Button>
      </div>

      {error ? <p className="mb-3 text-sm text-destructive">{error}</p> : null}

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Start</TableHead>
            <TableHead>End</TableHead>
            <TableHead>Price</TableHead>
            <TableHead />
          </TableRow>
        </TableHeader>
        <TableBody>
          {(slotsQuery.data ?? []).map((slot) => (
            <TableRow key={slot.id}>
              <TableCell>{new Date(slot.startTime).toLocaleString()}</TableCell>
              <TableCell>{new Date(slot.endTime).toLocaleString()}</TableCell>
              <TableCell>
                {slot.price} {slot.status}
              </TableCell>
              <TableCell className="text-right">
                <Button
                  size="sm"
                  disabled={busySlotId === slot.id}
                  onClick={() => void bookSlot(slot.id)}
                >
                  {busySlotId === slot.id ? "Booking…" : "Book"}
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      {!slotsQuery.isLoading && (slotsQuery.data?.length ?? 0) === 0 ? (
        <p className="mt-4 text-sm text-muted-foreground">No available slots for this date.</p>
      ) : null}
    </AppShell>
  )
}
