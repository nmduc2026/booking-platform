import { AppShell } from "@/components/app-shell"
import { useAuth } from "@/features/auth/auth-context"

export function ManagerHomePage() {
  const { user } = useAuth()

  return (
    <AppShell
      title="Shop manager"
      nav={[
        { to: "/manager", label: "Overview" },
        { to: "/manager/venue", label: "Venue" },
        { to: "/manager/bookings", label: "Bookings" },
      ]}
    >
      <p className="text-muted-foreground">
        Manage resources and slots for shops assigned to {user?.email}.
      </p>
      <ul className="mt-4 list-disc space-y-1 pl-5 text-sm">
        {(user?.shopIds ?? []).map((id) => (
          <li key={id}>
            <code>{id}</code>
          </li>
        ))}
      </ul>
    </AppShell>
  )
}
