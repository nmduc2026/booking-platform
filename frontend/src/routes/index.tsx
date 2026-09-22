import { Route, Routes } from "react-router"

import { HomePage } from "@/pages/home-page"

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
    </Routes>
  )
}
