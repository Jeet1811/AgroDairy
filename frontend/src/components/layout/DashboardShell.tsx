import { useState, type ReactNode } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import type { LucideIcon } from 'lucide-react'
import { LogOut, Menu, Milk, X } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { NotificationBell } from '@/components/layout/NotificationBell'
import { cn } from '@/lib/cn'

export interface NavItem {
  label: string
  to: string
  icon: LucideIcon
  end?: boolean
}

interface DashboardShellProps {
  navItems: NavItem[]
  brandLabel: string
}

export function DashboardShell({ navItems, brandLabel }: DashboardShellProps) {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/')
  }

  const sidebarContent: ReactNode = (
    <>
      <div className="flex h-16 items-center gap-2 px-5">
        <div className="rounded-lg bg-brand-600 p-1.5">
          <Milk className="h-5 w-5 text-white" />
        </div>
        <div className="leading-tight">
          <p className="text-sm font-bold text-brand-900">AgroDairy AI</p>
          <p className="text-xs text-brand-500">{brandLabel}</p>
        </div>
      </div>
      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-2">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition',
                isActive ? 'bg-brand-600 text-white shadow-sm' : 'text-brand-700 hover:bg-brand-50',
              )
            }
          >
            <item.icon className="h-4.5 w-4.5 shrink-0" />
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-brand-100 p-4">
        <p className="truncate text-sm font-semibold text-brand-900">{user?.fullName}</p>
        <p className="truncate text-xs text-brand-500">{user?.email}</p>
        <button onClick={handleLogout} className="btn-ghost mt-3 w-full !justify-start !px-2">
          <LogOut className="h-4 w-4" /> Sign out
        </button>
      </div>
    </>
  )

  return (
    <div className="min-h-screen bg-cream-50 lg:flex">
      {/* Desktop sidebar */}
      <aside className="hidden w-64 shrink-0 flex-col border-r border-brand-100 bg-white lg:flex">{sidebarContent}</aside>

      {/* Mobile sidebar drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="fixed inset-0 bg-brand-900/40" onClick={() => setMobileOpen(false)} />
          <aside className="relative flex h-full w-72 max-w-[80vw] flex-col bg-white shadow-soft-lg">
            <button
              onClick={() => setMobileOpen(false)}
              className="absolute right-3 top-4 rounded-full p-1.5 text-brand-500 hover:bg-brand-50"
              aria-label="Close menu"
            >
              <X className="h-5 w-5" />
            </button>
            {sidebarContent}
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between gap-3 border-b border-brand-100 bg-white/90 px-4 backdrop-blur sm:px-6">
          <button
            onClick={() => setMobileOpen(true)}
            className="rounded-lg p-2 text-brand-700 hover:bg-brand-50 lg:hidden"
            aria-label="Open menu"
          >
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex-1" />
          <NotificationBell />
        </header>
        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
