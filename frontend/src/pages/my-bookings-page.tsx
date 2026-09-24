import { useQuery } from "@tanstack/react-query"
import { Link } from "react-router"

import { AppShell } from "@/components/app-shell"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { listMyBookings } from "@/features/booking/booking-api"

const userNav = [
  { to: "/app", label: "Home" },
  { to: "/app/shops", label: "Find shops" },
  { to: "/app/bookings", label: "My bookings" },
]

export function MyBookingsPage() {
  const bookingsQuery = useQuery({
    queryKey: ["my-bookings"],
    queryFn: listMyBookings,
  })

  return (
    <AppShell title="My bookings" nav={userNav}>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Created</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Amount</TableHead>
            <TableHead />
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
              <TableCell className="text-right">
                {booking.status === "PENDING" ? (
                  <Button
                    size="sm"
                    nativeButton={false}
                    render={<Link to={`/app/bookings/${booking.id}/checkout`} />}
                  >
                    Pay
                  </Button>
                ) : null}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AppShell>
  )
}
