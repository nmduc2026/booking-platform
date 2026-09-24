import { Navigate, Outlet, useLocation } from "react-router"

import { useAuth } from "@/features/auth/auth-context"
import type { Role } from "@/lib/auth-storage"

export function RequireAuth({ roles }: { roles?: Role[] }) {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
