import { PaymentElement, useElements, useStripe } from "@stripe/react-stripe-js"
import { Elements } from "@stripe/react-stripe-js"
import { useMutation, useQuery } from "@tanstack/react-query"
import { useEffect, useState } from "react"
import { Link, useParams } from "react-router"

import { AppShell } from "@/components/app-shell"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { getBooking } from "@/features/booking/booking-api"
import { createPaymentIntent } from "@/features/booking/payment-api"
import { stripePromise } from "@/lib/stripe"

const userNav = [
  { to: "/app", label: "Home" },
  { to: "/app/shops", label: "Find shops" },
  { to: "/app/bookings", label: "My bookings" },
]

function CheckoutForm({ clientSecret }: { clientSecret: string }) {
  const stripe = useStripe()
  const elements = useElements()
  const [message, setMessage] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function onPay() {
    if (!stripe || !elements) return
    setSubmitting(true)
    setMessage(null)
    const result = await stripe.confirmPayment({
      elements,
      confirmParams: {
        return_url: `${window.location.origin}/app/bookings`,
      },
      redirect: "if_required",
    })
    if (result.error) {
      setMessage(result.error.message ?? "Payment failed")
    } else {
      setMessage("Payment submitted. Booking will confirm shortly.")
    }
    setSubmitting(false)
  }

  return (
    <div className="space-y-4">
      <PaymentElement options={{ layout: "tabs" }} />
      <Button disabled={!stripe || submitting} onClick={() => void onPay()}>
        {submitting ? "Processing…" : "Pay now"}
      </Button>
      {message ? <p className="text-sm text-muted-foreground">{message}</p> : null}
      <p className="text-xs text-muted-foreground">client_secret ready: {clientSecret.slice(0, 12)}…</p>
    </div>
  )
}

export function CheckoutPage() {
  const { bookingId = "" } = useParams()
  const bookingQuery = useQuery({
    queryKey: ["booking", bookingId],
    queryFn: () => getBooking(bookingId),
    enabled: Boolean(bookingId),
  })

  const intentMutation = useMutation({
    mutationFn: () =>
      createPaymentIntent(bookingId, Number(bookingQuery.data?.amount ?? 0)),
  })

  useEffect(() => {
    if (bookingQuery.data?.status === "PENDING" && !intentMutation.data && !intentMutation.isPending) {
      intentMutation.mutate()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bookingQuery.data?.id, bookingQuery.data?.status])

  const clientSecret = intentMutation.data?.clientSecret

  return (
    <AppShell title="Checkout" nav={userNav}>
      <Card className="max-w-xl">
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Booking payment</CardTitle>
          {bookingQuery.data ? <Badge>{bookingQuery.data.status}</Badge> : null}
        </CardHeader>
        <CardContent className="space-y-4">
          {bookingQuery.data ? (
            <p className="text-sm text-muted-foreground">
              Amount: {bookingQuery.data.amount} · Slot {bookingQuery.data.slotId.slice(0, 8)}
            </p>
          ) : null}

          {!stripePromise ? (
            <p className="text-sm text-destructive">
              Set VITE_STRIPE_PUBLISHABLE_KEY to enable Stripe Elements.
            </p>
          ) : null}

          {intentMutation.isError ? (
            <p className="text-sm text-destructive">Could not create payment intent.</p>
          ) : null}

          {clientSecret && stripePromise ? (
            <Elements stripe={stripePromise} options={{ clientSecret }}>
              <CheckoutForm clientSecret={clientSecret} />
            </Elements>
          ) : (
            <p className="text-sm text-muted-foreground">Preparing payment…</p>
          )}

          <Button variant="outline" nativeButton={false} render={<Link to="/app/bookings" />}>
            My bookings
          </Button>
        </CardContent>
      </Card>
    </AppShell>
  )
}
