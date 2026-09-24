import { useState } from "react"
import { Link, useNavigate } from "react-router"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { login } from "@/features/auth/api"
import { useAuth } from "@/features/auth/auth-context"
import { ApiError } from "@/lib/api"
import { homePathForRole } from "@/lib/auth-storage"
import { useForm, z, zodResolver } from "@/lib/form"

const schema = z.object({
  email: z.email(),
  password: z.string().min(8),
})

type FormValues = z.infer<typeof schema>

export function LoginForm() {
  const navigate = useNavigate()
  const { setAuth } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  })

  async function onSubmit(values: FormValues) {
    setError(null)
    try {
      const auth = await login(values.email, values.password)
      setAuth(auth)
      navigate(homePathForRole(auth.user.role), { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.body || err.message : "Login failed")
    }
  }

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle className="text-xl">Welcome back</CardTitle>
        <CardDescription>Sign in to book or manage your spa</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={form.handleSubmit(onSubmit)}>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input id="email" type="email" {...form.register("email")} />
              <FieldError>{form.formState.errors.email?.message}</FieldError>
            </Field>
            <Field>
              <FieldLabel htmlFor="password">Password</FieldLabel>
              <Input id="password" type="password" {...form.register("password")} />
              <FieldError>{form.formState.errors.password?.message}</FieldError>
            </Field>
            {error ? <FieldError>{error}</FieldError> : null}
            <Field>
              <Button type="submit" disabled={form.formState.isSubmitting}>
                {form.formState.isSubmitting ? "Signing in…" : "Sign in"}
              </Button>
              <FieldDescription className="text-center">
                No account? <Link to="/register">Create one</Link>
              </FieldDescription>
            </Field>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
