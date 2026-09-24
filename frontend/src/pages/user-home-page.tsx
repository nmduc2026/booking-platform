import { Link } from "react-router"

import { AppShell } from "@/components/app-shell"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/features/auth/auth-context"

export function UserHomePage() {
  const { user } = useAuth()

  return (
    <AppShell
      title={`Hello, ${user?.fullName ?? "guest"}`}
      nav={[
        { to: "/app", label: "Home" },
        { to: "/app/shops", label: "Find shops" },
        { to: "/app/bookings", label: "My bookings" },
      ]}
    >
      <p className="mb-4 text-muted-foreground">
        Browse spa shops, pick an available slot, and pay with Stripe.
      </p>
      <Button nativeButton={false} render={<Link to="/app/shops" />}>
        Find shops
      </Button>
    </AppShell>
  )
}
