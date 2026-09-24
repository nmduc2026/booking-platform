import { useMutation } from "@tanstack/react-query"
import { useState } from "react"
import { toast } from "sonner"

import { AppShell } from "@/components/app-shell"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { createAdminUser, mapUserToShop } from "@/features/auth/api"
import { getApiErrorMessage } from "@/lib/api-message"

const nav = [
  { to: "/admin", label: "Overview" },
  { to: "/admin/shops", label: "Shops" },
  { to: "/admin/users", label: "Users" },
]

export function AdminUsersPage() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [fullName, setFullName] = useState("")
  const [phone, setPhone] = useState("")
  const [role, setRole] = useState("SHOP_MANAGER")
  const [userId, setUserId] = useState("")
  const [shopId, setShopId] = useState("")

  const createUserMutation = useMutation({
    mutationFn: () =>
      createAdminUser({
        email,
        password,
        fullName,
        phone: phone || undefined,
        role,
      }),
    onSuccess: (user) => {
      toast.success(`Created user ${user.email}`)
      setUserId(user.id)
    },
    onError: (err) => {
      toast.error(getApiErrorMessage(err, "Create failed"))
    },
  })

  const mapMutation = useMutation({
    mutationFn: () => mapUserToShop({ userId, shopId }),
    onSuccess: (mapping) => {
      toast.success(`Mapped user to shop ${mapping.shopId}`)
    },
    onError: (err) => {
      toast.error(getApiErrorMessage(err, "Mapping failed"))
    },
  })

  return (
    <AppShell title="Users & shop mapping" nav={nav}>
      <section className="mb-10 max-w-lg space-y-3">
        <h2 className="text-lg font-medium">Create user</h2>
        <Input placeholder="Full name" value={fullName} onChange={(e) => setFullName(e.target.value)} />
        <Input placeholder="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <Input placeholder="Phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
        <Input
          placeholder="Password (min 8)"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Select value={role} onValueChange={(v) => setRole(v ?? "SHOP_MANAGER")}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="USER">USER</SelectItem>
            <SelectItem value="SHOP_MANAGER">SHOP_MANAGER</SelectItem>
            <SelectItem value="ADMIN">ADMIN</SelectItem>
          </SelectContent>
        </Select>
        <Button
          disabled={!email || !password || !fullName || createUserMutation.isPending}
          onClick={() => createUserMutation.mutate()}
        >
          Create user
        </Button>
      </section>

      <section className="max-w-lg space-y-3">
        <h2 className="text-lg font-medium">Assign shop manager</h2>
        <Input placeholder="User ID" value={userId} onChange={(e) => setUserId(e.target.value)} />
        <Input placeholder="Shop ID" value={shopId} onChange={(e) => setShopId(e.target.value)} />
        <Button
          disabled={!userId || !shopId || mapMutation.isPending}
          onClick={() => mapMutation.mutate()}
        >
          Map user to shop
        </Button>
      </section>
    </AppShell>
  )
}
