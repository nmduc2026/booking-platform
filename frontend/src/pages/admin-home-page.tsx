import { AppShell } from "@/components/app-shell"

export function AdminHomePage() {
  return (
    <AppShell
      title="Admin"
      nav={[
        { to: "/admin", label: "Overview" },
        { to: "/admin/shops", label: "Shops" },
        { to: "/admin/users", label: "Users" },
      ]}
    >
      <p className="text-muted-foreground">
        Create shops, managers, and shop assignments.
      </p>
    </AppShell>
  )
}
