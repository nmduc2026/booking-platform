import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { toast } from "sonner"
import { LinkIcon, PlusIcon } from "lucide-react"

import { AppShell } from "@/components/app-shell"
import { TablePagination } from "@/components/table-pagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { createAdminUser, listAdminUsers, mapUserToShop } from "@/features/auth/api"
import { getApiErrorMessage } from "@/lib/api-message"

const PAGE_SIZE = 10

export function AdminUsersPage() {
  const [page, setPage] = useState(0)
  const [q, setQ] = useState("")
  const [search, setSearch] = useState("")
  const [roleFilter, setRoleFilter] = useState<string>("ALL")
  const [createOpen, setCreateOpen] = useState(false)
  const [mapOpen, setMapOpen] = useState(false)
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [fullName, setFullName] = useState("")
  const [phone, setPhone] = useState("")
  const [role, setRole] = useState("SHOP_MANAGER")
  const [userId, setUserId] = useState("")
  const [shopId, setShopId] = useState("")
  const queryClient = useQueryClient()

  const usersQuery = useQuery({
    queryKey: ["admin-users", page, search, roleFilter],
    queryFn: () =>
      listAdminUsers({
        page,
        size: PAGE_SIZE,
        q: search || undefined,
        role: roleFilter === "ALL" ? undefined : roleFilter,
      }),
  })

  function resetCreateForm() {
    setEmail("")
    setPassword("")
    setFullName("")
    setPhone("")
    setRole("SHOP_MANAGER")
  }

  function resetMapForm() {
    setUserId("")
    setShopId("")
  }

  const createUserMutation = useMutation({
    mutationFn: () =>
      createAdminUser({
        email,
        password,
        fullName,
        phone: phone || undefined,
        role,
      }),
    onSuccess: async (user) => {
      toast.success(`Created user ${user.email}`)
      resetCreateForm()
      setCreateOpen(false)
      setUserId(user.id)
      setPage(0)
      await queryClient.invalidateQueries({ queryKey: ["admin-users"] })
    },
    onError: (err) => {
      toast.error(getApiErrorMessage(err, "Create failed"))
    },
  })

  const mapMutation = useMutation({
    mutationFn: () => mapUserToShop({ userId, shopId }),
    onSuccess: async (mapping) => {
      toast.success(`Mapped user to shop ${mapping.shopId}`)
      resetMapForm()
      setMapOpen(false)
      await queryClient.invalidateQueries({ queryKey: ["admin-users"] })
    },
    onError: (err) => {
      toast.error(getApiErrorMessage(err, "Mapping failed"))
    },
  })

  const data = usersQuery.data

  return (
    <AppShell title="Users">
      <section className="space-y-4">
        <div className="flex flex-wrap items-end gap-2">
          <div className="min-w-64 flex-1">
            <Input
              placeholder="Search by email or name"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  setPage(0)
                  setSearch(q.trim())
                }
              }}
            />
          </div>
          <Select
            value={roleFilter}
            onValueChange={(v) => {
              setPage(0)
              setRoleFilter(v ?? "ALL")
            }}
          >
            <SelectTrigger className="w-44">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All roles</SelectItem>
              <SelectItem value="USER">USER</SelectItem>
              <SelectItem value="SHOP_MANAGER">SHOP_MANAGER</SelectItem>
              <SelectItem value="ADMIN">ADMIN</SelectItem>
            </SelectContent>
          </Select>
          <Button
            variant="outline"
            onClick={() => {
              setPage(0)
              setSearch(q.trim())
            }}
          >
            Search
          </Button>
          <Button variant="outline" onClick={() => setMapOpen(true)}>
            <LinkIcon />
            Assign shop
          </Button>
          <Button onClick={() => setCreateOpen(true)}>
            <PlusIcon />
            Create user
          </Button>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Shops</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>ID</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {(data?.content ?? []).map((user) => (
              <TableRow key={user.id}>
                <TableCell className="font-medium">{user.fullName}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  <Badge variant="secondary">{user.role}</Badge>
                </TableCell>
                <TableCell className="font-mono text-xs">
                  {user.shopIds.length ? user.shopIds.join(", ") : "—"}
                </TableCell>
                <TableCell>{user.status}</TableCell>
                <TableCell>
                  <button
                    type="button"
                    className="font-mono text-xs underline-offset-2 hover:underline"
                    onClick={() => {
                      setUserId(user.id)
                      setMapOpen(true)
                      toast.message("User selected for shop mapping")
                    }}
                  >
                    {user.id}
                  </button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        {!usersQuery.isLoading && (data?.content.length ?? 0) === 0 ? (
          <p className="text-sm text-muted-foreground">No users found.</p>
        ) : null}

        <TablePagination
          page={data?.page ?? page}
          totalPages={data?.totalPages ?? 1}
          totalElements={data?.totalElements ?? 0}
          onPageChange={setPage}
        />
      </section>

      <Dialog
        open={createOpen}
        onOpenChange={(open) => {
          setCreateOpen(open)
          if (!open) resetCreateForm()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create user</DialogTitle>
            <DialogDescription>
              Create an account with the selected role.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <Input
              placeholder="Full name"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
            <Input
              placeholder="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <Input
              placeholder="Phone"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
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
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button
              disabled={!email || !password || !fullName || createUserMutation.isPending}
              onClick={() => createUserMutation.mutate()}
            >
              {createUserMutation.isPending ? "Creating…" : "Create"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={mapOpen}
        onOpenChange={(open) => {
          setMapOpen(open)
          if (!open) resetMapForm()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Assign shop manager</DialogTitle>
            <DialogDescription>
              Map a SHOP_MANAGER user to a shop ID.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <Input
              placeholder="User ID"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
            />
            <Input
              placeholder="Shop ID"
              value={shopId}
              onChange={(e) => setShopId(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setMapOpen(false)}>
              Cancel
            </Button>
            <Button
              disabled={!userId || !shopId || mapMutation.isPending}
              onClick={() => mapMutation.mutate()}
            >
              {mapMutation.isPending ? "Mapping…" : "Map user"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </AppShell>
  )
}
