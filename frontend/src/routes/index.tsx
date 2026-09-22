import { Navigate, Route, Routes } from "react-router"

import { DashboardPage } from "@/pages/dashboard-page"
import { LoginPage } from "@/pages/login-page"

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
