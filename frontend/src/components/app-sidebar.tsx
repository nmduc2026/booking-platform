"use client"

import * as React from "react"

import { NavMain } from "@/components/nav-main"
import { NavProjects } from "@/components/nav-projects"
import { NavUser } from "@/components/nav-user"
import { TeamSwitcher } from "@/components/team-switcher"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from "@/components/ui/sidebar"
import { AudioLinesIcon, TerminalIcon, TerminalSquareIcon, BotIcon, BookOpenIcon, Settings2Icon, FrameIcon, PieChartIcon, MapIcon } from "lucide-react"

// This is sample data.
const data = {
  user: {
    name: "Shop Manager",
    email: "manager@spa.com",
    avatar: "",
  },
  teams: [
    {
      name: "Booking Platform",
      logo: (
        <img src="/logo.svg" alt="" className="size-4 object-contain" />
      ),
      plan: "Admin",
    },
    {
      name: "Serenity Spa",
      logo: <AudioLinesIcon />,
      plan: "Shop Manager",
    },
    {
      name: "Lotus Wellness",
      logo: <TerminalIcon />,
      plan: "Shop Manager",
    },
  ],
  navMain: [
    {
      title: "Bookings",
      url: "#",
      icon: <TerminalSquareIcon />,
      isActive: true,
      items: [
        { title: "All bookings", url: "#" },
        { title: "Pending", url: "#" },
        { title: "Confirmed", url: "#" },
      ],
    },
    {
      title: "Venue",
      url: "#",
      icon: <BotIcon />,
      items: [
        { title: "Shops", url: "#" },
        { title: "Resources", url: "#" },
        { title: "Time slots", url: "#" },
      ],
    },
    {
      title: "Payments",
      url: "#",
      icon: <BookOpenIcon />,
      items: [
        { title: "Transactions", url: "#" },
        { title: "Refunds", url: "#" },
      ],
    },
    {
      title: "Settings",
      url: "#",
      icon: <Settings2Icon />,
      items: [
        { title: "General", url: "#" },
        { title: "Users", url: "#" },
        { title: "Roles", url: "#" },
      ],
    },
  ],
  projects: [
    {
      name: "Serenity Spa",
      url: "#",
      icon: <FrameIcon />,
    },
    {
      name: "Lotus Wellness",
      url: "#",
      icon: <PieChartIcon />,
    },
    {
      name: "Zen Garden",
      url: "#",
      icon: <MapIcon />,
    },
  ],
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <TeamSwitcher teams={data.teams} />
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={data.navMain} />
        <NavProjects projects={data.projects} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={data.user} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
