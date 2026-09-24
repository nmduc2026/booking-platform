import { Link } from "react-router"
import type { ReactNode } from "react"

import { Button } from "@/components/ui/button"
import { useAuth } from "@/features/auth/auth-context"

type AppShellProps = {
  title: string
  nav: { to: string; label: string }[]
  children: ReactNode
}

export function AppShell({ title, nav, children }: AppShellProps) {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-svh bg-background text-foreground">
      <header className="border-b">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-2 font-semibold">
              <img src="/logo.svg" alt="" className="h-6 w-auto" />
              Booking Platform
            </Link>
            <nav className="hidden items-center gap-3 text-sm md:flex">
              {nav.map((item) => (
                <Link
                  key={item.to}
                  to={item.to}
                  className="text-muted-foreground transition-colors hover:text-foreground"
                >
                  {item.label}
                </Link>
              ))}
            </nav>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <div className="hidden text-right sm:block">
              <div className="font-medium">{user?.fullName}</div>
              <div className="text-muted-foreground">{user?.role}</div>
            </div>
            <Button variant="outline" size="sm" onClick={logout}>
              Log out
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <h1 className="mb-6 text-2xl font-semibold tracking-tight">{title}</h1>
        {children}
      </main>
    </div>
  )
}
