import { Navigate, Route, Routes } from "react-router"

import { useAuth } from "@/features/auth/auth-context"
import { homePathForRole } from "@/lib/auth-storage"
import { AdminHomePage } from "@/pages/admin-home-page"
import { CheckoutPage } from "@/pages/checkout-page"
import { LoginPage } from "@/pages/login-page"
import { ManagerBookingsPage } from "@/pages/manager-bookings-page"
import { ManagerHomePage } from "@/pages/manager-home-page"
import { ManagerVenuePage } from "@/pages/manager-venue-page"
import { MyBookingsPage } from "@/pages/my-bookings-page"
import { RegisterPage } from "@/pages/register-page"
import { ShopSlotsPage } from "@/pages/shop-slots-page"
import { ShopsPage } from "@/pages/shops-page"
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
        <Route path="/app/shops" element={<ShopsPage />} />
        <Route path="/app/shops/:shopId" element={<ShopSlotsPage />} />
        <Route path="/app/bookings" element={<MyBookingsPage />} />
        <Route path="/app/bookings/:bookingId/checkout" element={<CheckoutPage />} />
      </Route>

      <Route element={<RequireAuth roles={["SHOP_MANAGER"]} />}>
        <Route path="/manager" element={<ManagerHomePage />} />
        <Route path="/manager/venue" element={<ManagerVenuePage />} />
        <Route path="/manager/bookings" element={<ManagerBookingsPage />} />
      </Route>

      <Route element={<RequireAuth roles={["ADMIN"]} />}>
        <Route path="/admin" element={<AdminHomePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
