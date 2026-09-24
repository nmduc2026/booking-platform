import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useMemo, useState } from "react"

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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useAuth } from "@/features/auth/auth-context"
import {
  createResource,
  createSlot,
  listShopResources,
} from "@/features/booking/venue-api"
import { getApiErrorMessage } from "@/lib/api-message"
import { toast } from "sonner"

const nav = [
  { to: "/manager", label: "Overview" },
  { to: "/manager/venue", label: "Venue" },
  { to: "/manager/bookings", label: "Bookings" },
]

export function ManagerVenuePage() {
  const { user } = useAuth()
  const shopIds = user?.shopIds ?? []
  const [shopId, setShopId] = useState(shopIds[0] ?? "")
  const [resourceName, setResourceName] = useState("")
  const [resourceType, setResourceType] = useState("ROOM")
  const [slotResourceId, setSlotResourceId] = useState("")
  const [startTime, setStartTime] = useState("")
  const [endTime, setEndTime] = useState("")
  const [price, setPrice] = useState("50")
  const queryClient = useQueryClient()

  const resourcesQuery = useQuery({
    queryKey: ["resources", shopId],
    queryFn: () => listShopResources(shopId),
    enabled: Boolean(shopId),
  })

  const resources = resourcesQuery.data ?? []
  const selectedSlotResource = useMemo(
    () => slotResourceId || resources[0]?.id || "",
    [slotResourceId, resources]
  )

  const createResourceMutation = useMutation({
    mutationFn: () =>
      createResource(shopId, { name: resourceName, type: resourceType }),
    onSuccess: async () => {
      setResourceName("")
      toast.success("Resource created")
      await queryClient.invalidateQueries({ queryKey: ["resources", shopId] })
    },
    onError: (err) => toast.error(getApiErrorMessage(err, "Failed to create resource")),
  })

  const createSlotMutation = useMutation({
    mutationFn: () =>
      createSlot(shopId, {
        resourceId: selectedSlotResource,
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        price: Number(price),
      }),
    onSuccess: () => {
      setStartTime("")
      setEndTime("")
      toast.success("Slot created")
    },
    onError: (err) => toast.error(getApiErrorMessage(err, "Failed to create slot")),
  })

  return (
    <AppShell title="Venue management" nav={nav}>
      <div className="mb-6 flex flex-wrap gap-3">
        <Select value={shopId} onValueChange={(v) => setShopId(v ?? "")}>
          <SelectTrigger className="w-80">
            <SelectValue placeholder="Select shop" />
          </SelectTrigger>
          <SelectContent>
            {shopIds.map((id) => (
              <SelectItem key={id} value={id}>
                {id}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <section className="mb-10 space-y-3">
        <h2 className="text-lg font-medium">Resources</h2>
        <div className="flex flex-wrap gap-2">
          <Input
            placeholder="Resource name"
            value={resourceName}
            onChange={(e) => setResourceName(e.target.value)}
            className="w-56"
          />
          <Select value={resourceType} onValueChange={(v) => setResourceType(v ?? "ROOM")}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ROOM">ROOM</SelectItem>
              <SelectItem value="STAFF">STAFF</SelectItem>
              <SelectItem value="EQUIPMENT">EQUIPMENT</SelectItem>
            </SelectContent>
          </Select>
          <Button
            disabled={!shopId || !resourceName || createResourceMutation.isPending}
            onClick={() => createResourceMutation.mutate()}
          >
            Add resource
          </Button>
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>ID</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {resources.map((resource) => (
              <TableRow key={resource.id}>
                <TableCell>{resource.name}</TableCell>
                <TableCell>{resource.type}</TableCell>
                <TableCell className="font-mono text-xs">{resource.id}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-medium">Create time slot</h2>
        <div className="grid gap-2 md:grid-cols-2">
          <Select
            value={selectedSlotResource}
            onValueChange={(v) => setSlotResourceId(v ?? "")}
          >
            <SelectTrigger>
              <SelectValue placeholder="Resource" />
            </SelectTrigger>
            <SelectContent>
              {resources.map((resource) => (
                <SelectItem key={resource.id} value={resource.id}>
                  {resource.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Input
            type="number"
            step="0.01"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            placeholder="Price"
          />
          <Input
            type="datetime-local"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
          />
          <Input
            type="datetime-local"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
          />
        </div>
        <Button
          disabled={
            !shopId ||
            !selectedSlotResource ||
            !startTime ||
            !endTime ||
            createSlotMutation.isPending
          }
          onClick={() => createSlotMutation.mutate()}
        >
          Add slot
        </Button>
      </section>
    </AppShell>
  )
}
