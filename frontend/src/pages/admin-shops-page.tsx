import { useMutation } from "@tanstack/react-query"
import { useState } from "react"
import { toast } from "sonner"

import { AppShell } from "@/components/app-shell"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { createShop } from "@/features/booking/venue-api"
import { getApiErrorMessage } from "@/lib/api-message"

const nav = [
  { to: "/admin", label: "Overview" },
  { to: "/admin/shops", label: "Shops" },
  { to: "/admin/users", label: "Users" },
]

export function AdminShopsPage() {
  const [name, setName] = useState("")
  const [address, setAddress] = useState("")
  const [description, setDescription] = useState("")

  const mutation = useMutation({
    mutationFn: () =>
      createShop({
        name,
        address: address || undefined,
        description: description || undefined,
      }),
    onSuccess: (shop) => {
      toast.success(`Created shop ${shop.name}`)
      setName("")
      setAddress("")
      setDescription("")
    },
    onError: (err) => {
      toast.error(getApiErrorMessage(err, "Create failed"))
    },
  })

  return (
    <AppShell title="Create shop" nav={nav}>
      <div className="max-w-lg space-y-3">
        <Input
          placeholder="Shop name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <Input
          placeholder="Address"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
        />
        <Textarea
          placeholder="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <Button
          disabled={!name || mutation.isPending}
          onClick={() => mutation.mutate()}
        >
          Create shop
        </Button>
      </div>
    </AppShell>
  )
}
