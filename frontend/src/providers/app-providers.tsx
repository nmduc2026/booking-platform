import { QueryClientProvider } from "@tanstack/react-query"
import { Elements } from "@stripe/react-stripe-js"
import type { ReactNode } from "react"
import { BrowserRouter } from "react-router"

import { ThemeProvider } from "@/components/theme-provider"
import { queryClient } from "@/lib/query-client"
import { stripePromise } from "@/lib/stripe"

type AppProvidersProps = {
  children: ReactNode
}

export function AppProviders({ children }: AppProvidersProps) {
  const content = (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ThemeProvider>{children}</ThemeProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )

  if (!stripePromise) {
    return content
  }

  return <Elements stripe={stripePromise}>{content}</Elements>
}
