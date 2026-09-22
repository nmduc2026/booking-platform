import { Link } from "react-router"

import { LoginForm } from "@/components/login-form"

export function LoginPage() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted p-6 md:p-10">
      <div className="flex w-full max-w-sm flex-col gap-6">
        <Link
          to="/login"
          className="flex items-center gap-2 self-center font-medium"
        >
          <img
            src="/logo.svg"
            alt="Booking Platform"
            className="h-6 w-auto"
          />
          Booking Platform
        </Link>
        <LoginForm />
      </div>
    </div>
  )
}
