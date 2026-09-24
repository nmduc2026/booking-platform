import { api } from "@/lib/api"

export type PaymentIntent = {
  paymentId: string
  bookingId: string
  stripePaymentIntentId: string
  clientSecret: string
  amount: number
  currency: string
  status: string
  createdAt: string
}

export function createPaymentIntent(bookingId: string, amount: number, currency = "usd") {
  return api<PaymentIntent>("/api/payment/payments/intent", {
    method: "POST",
    body: JSON.stringify({ bookingId, amount, currency }),
  })
}
