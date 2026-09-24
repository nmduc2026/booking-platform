import { useQuery, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"

import { AppShell } from "@/components/app-shell"
import { Badge } from "@/components/ui/badge"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useAuth } from "@/features/auth/auth-context"
import { listShopBookings } from "@/features/booking/booking-api"
import { useShopBookingStream } from "@/hooks/use-shop-booking-stream"

const nav = [
  { to: "/manager", label: "Overview" },
  { to: "/manager/venue", label: "Venue" },
  { to: "/manager/bookings", label: "Bookings" },
]

export function ManagerBookingsPage() {
  const { user } = useAuth()
  const shopIds = user?.shopIds ?? []
  const [shopId, setShopId] = useState(shopIds[0] ?? "")
  const queryClient = useQueryClient()

  const bookingsQuery = useQuery({
    queryKey: ["shop-bookings", shopId],
    queryFn: () => listShopBookings(shopId),
    enabled: Boolean(shopId),
  })

  useShopBookingStream(shopId, () => {
    void queryClient.invalidateQueries({ queryKey: ["shop-bookings", shopId] })
  })

  return (
    <AppShell title="Shop bookings" nav={nav}>
      <div className="mb-4">
        <Select value={shopId} onValueChange={(v) => setShopId(v ?? "")}>
          <SelectTrigger className="w-80">
            <SelectValue placeholder="Select shop" />
          </SelectTrigger>
          <SelectContent>
            {shopIds.map((id) => (
              <SelectItem key={id} value={id}>
                {id}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Created</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Amount</TableHead>
            <TableHead>Slot</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {(bookingsQuery.data ?? []).map((booking) => (
            <TableRow key={booking.id}>
              <TableCell>{new Date(booking.createdAt).toLocaleString()}</TableCell>
              <TableCell>
                <Badge variant="secondary">{booking.status}</Badge>
              </TableCell>
              <TableCell>{booking.amount}</TableCell>
              <TableCell className="font-mono text-xs">{booking.slotId}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AppShell>
  )
}
