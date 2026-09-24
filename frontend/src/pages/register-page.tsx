import { Link } from "react-router"

import { RegisterForm } from "@/features/auth/register-form"

export function RegisterPage() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted p-6 md:p-10">
      <div className="flex w-full max-w-sm flex-col gap-6">
        <Link to="/register" className="flex items-center gap-2 self-center font-medium">
          <img src="/logo.svg" alt="Booking Platform" className="h-6 w-auto" />
          Booking Platform
        </Link>
        <RegisterForm />
      </div>
    </div>
  )
}
