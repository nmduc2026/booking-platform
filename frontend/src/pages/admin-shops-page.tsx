import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { toast } from "sonner"
import { PlusIcon } from "lucide-react"

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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import { createShop, listAdminShops } from "@/features/booking/venue-api"
import { getApiErrorMessage } from "@/lib/api-message"

const PAGE_SIZE = 10

export function AdminShopsPage() {
  const [page, setPage] = useState(0)
  const [q, setQ] = useState("")
  const [search, setSearch] = useState("")
  const [createOpen, setCreateOpen] = useState(false)
  const [name, setName] = useState("")
  const [address, setAddress] = useState("")
  const [description, setDescription] = useState("")
  const queryClient = useQueryClient()

  const shopsQuery = useQuery({
    queryKey: ["admin-shops", page, search],
    queryFn: () => listAdminShops({ page, size: PAGE_SIZE, q: search || undefined }),
  })

  function resetCreateForm() {
    setName("")
    setAddress("")
    setDescription("")
  }

  const mutation = useMutation({
    mutationFn: () =>
      createShop({
        name,
        address: address || undefined,
        description: description || undefined,
      }),
    onSuccess: async (shop) => {
      toast.success(`Created shop ${shop.name}`)
      resetCreateForm()
      setCreateOpen(false)
      setPage(0)
      await queryClient.invalidateQueries({ queryKey: ["admin-shops"] })
    },
    onError: (err) => {
      toast.error(getApiErrorMessage(err, "Create failed"))
    },
  })

  const data = shopsQuery.data

  return (
    <AppShell title="Shops">
      <section className="space-y-4">
        <div className="flex flex-wrap items-end gap-2">
          <div className="min-w-64 flex-1">
            <Input
              placeholder="Search by name or address"
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
          <Button
            variant="outline"
            onClick={() => {
              setPage(0)
              setSearch(q.trim())
            }}
          >
            Search
          </Button>
          <Button onClick={() => setCreateOpen(true)}>
            <PlusIcon />
            Create shop
          </Button>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Address</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Created</TableHead>
              <TableHead>ID</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {(data?.content ?? []).map((shop) => (
              <TableRow key={shop.id}>
                <TableCell className="font-medium">{shop.name}</TableCell>
                <TableCell>{shop.address || "—"}</TableCell>
                <TableCell>
                  <Badge variant="secondary">{shop.status}</Badge>
                </TableCell>
                <TableCell>{new Date(shop.createdAt).toLocaleString()}</TableCell>
                <TableCell className="font-mono text-xs">{shop.id}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        {!shopsQuery.isLoading && (data?.content.length ?? 0) === 0 ? (
          <p className="text-sm text-muted-foreground">No shops found.</p>
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
            <DialogTitle>Create shop</DialogTitle>
            <DialogDescription>
              Add a new spa shop to the marketplace.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
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
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button
              disabled={!name || mutation.isPending}
              onClick={() => mutation.mutate()}
            >
              {mutation.isPending ? "Creating…" : "Create"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </AppShell>
  )
}
