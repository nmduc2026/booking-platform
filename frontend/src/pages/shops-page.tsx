import { useQuery } from "@tanstack/react-query"
import { Link } from "react-router"

import { AppShell } from "@/components/app-shell"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { listShops } from "@/features/booking/venue-api"

const userNav = [
  { to: "/app", label: "Home" },
  { to: "/app/shops", label: "Find shops" },
  { to: "/app/bookings", label: "My bookings" },
]

export function ShopsPage() {
  const shopsQuery = useQuery({
    queryKey: ["shops"],
    queryFn: listShops,
  })

  return (
    <AppShell title="Find a spa" nav={userNav}>
      {shopsQuery.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-36 rounded-xl" />
          <Skeleton className="h-36 rounded-xl" />
        </div>
      ) : null}

      {shopsQuery.isError ? (
        <p className="text-destructive">Could not load shops.</p>
      ) : null}

      <div className="grid gap-4 md:grid-cols-2">
        {(shopsQuery.data ?? []).map((shop) => (
          <Card key={shop.id}>
            <CardHeader>
              <div className="flex items-start justify-between gap-3">
                <CardTitle>{shop.name}</CardTitle>
                <Badge variant="secondary">{shop.status}</Badge>
              </div>
              <CardDescription>{shop.address || "Address coming soon"}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm text-muted-foreground">
                {shop.description || "No description yet."}
              </p>
              <Button nativeButton={false} render={<Link to={`/app/shops/${shop.id}`} />}>
                View slots
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </AppShell>
  )
}
