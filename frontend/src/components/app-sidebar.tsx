import { Link, useLocation } from "react-router"
import type { ReactNode } from "react"
import {
  CalendarDaysIcon,
  LayoutDashboardIcon,
  StoreIcon,
} from "lucide-react"

import { NavMain } from "@/components/nav-main"
import { NavUser } from "@/components/nav-user"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
} from "@/components/ui/sidebar"
import { useAuth } from "@/features/auth/auth-context"
import type { Role } from "@/lib/auth-storage"

type NavItem = {
  title: string
  url: string
  icon?: ReactNode
  isActive?: boolean
  items?: { title: string; url: string }[]
}

function navForRole(role: Role, pathname: string): NavItem[] {
  if (role === "ADMIN") {
    return [
      {
        title: "Admin",
        url: "/admin",
        icon: <LayoutDashboardIcon />,
        isActive: pathname.startsWith("/admin"),
        items: [
          { title: "Overview", url: "/admin" },
          { title: "Shops", url: "/admin/shops" },
          { title: "Users", url: "/admin/users" },
        ],
      },
    ]
  }

  if (role === "SHOP_MANAGER") {
    return [
      {
        title: "Manager",
        url: "/manager",
        icon: <StoreIcon />,
        isActive: pathname.startsWith("/manager"),
        items: [
          { title: "Overview", url: "/manager" },
          { title: "Venue", url: "/manager/venue" },
          { title: "Bookings", url: "/manager/bookings" },
        ],
      },
    ]
  }

  return [
    {
      title: "Booking",
      url: "/app",
      icon: <CalendarDaysIcon />,
      isActive: pathname.startsWith("/app"),
      items: [
        { title: "Home", url: "/app" },
        { title: "Find shops", url: "/app/shops" },
        { title: "My bookings", url: "/app/bookings" },
      ],
    },
  ]
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { user } = useAuth()
  const location = useLocation()
  const role = user?.role ?? "USER"
  const navMain = navForRole(role, location.pathname)

  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" render={<Link to="/" />}>
              <div className="flex aspect-square size-8 items-center justify-center">
                <img src="/logo.svg" alt="" className="size-8 object-contain" />
              </div>
              <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-medium">Booking Platform</span>
                <span className="truncate text-xs">{role}</span>
              </div>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={navMain} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser
          user={{
            name: user?.fullName ?? "User",
            email: user?.email ?? "",
            avatar: "",
            initials: (user?.fullName ?? "U")
              .split(" ")
              .map((part) => part[0])
              .join("")
              .slice(0, 2)
              .toUpperCase(),
          }}
        />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
