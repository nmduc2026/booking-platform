import { Navigate, Route, Routes } from "react-router"

import { useAuth } from "@/features/auth/auth-context"
import { homePathForRole } from "@/lib/auth-storage"
import { AdminHomePage } from "@/pages/admin-home-page"
import { LoginPage } from "@/pages/login-page"
import { ManagerHomePage } from "@/pages/manager-home-page"
import { RegisterPage } from "@/pages/register-page"
import { UserHomePage } from "@/pages/user-home-page"
import { RequireAuth } from "@/routes/require-auth"

function RootRedirect() {
  const { isAuthenticated, user } = useAuth()
  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />
  }
  return <Navigate to={homePathForRole(user.role)} replace />
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<RequireAuth roles={["USER"]} />}>
        <Route path="/app" element={<UserHomePage />} />
      </Route>

      <Route element={<RequireAuth roles={["SHOP_MANAGER"]} />}>
        <Route path="/manager" element={<ManagerHomePage />} />
      </Route>

      <Route element={<RequireAuth roles={["ADMIN"]} />}>
        <Route path="/admin" element={<AdminHomePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
