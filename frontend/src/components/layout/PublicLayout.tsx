import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { LayoutDashboard, LogOut, Menu, Milk, ShoppingCart, UserCircle, X } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { NotificationBell } from '@/components/layout/NotificationBell'
import { cn } from '@/lib/cn'

const NAV_LINKS = [
  { label: 'Home', to: '/' },
  { label: 'Shop', to: '/products' },
]

export function PublicLayout() {
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/')
  }

  return (
    <div className="flex min-h-screen flex-col bg-cream-50">
      <header className="sticky top-0 z-40 border-b border-brand-100 bg-white/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
          <Link to="/" className="flex items-center gap-2">
            <div className="rounded-lg bg-brand-600 p-1.5">
              <Milk className="h-5 w-5 text-white" />
            </div>
            <span className="text-lg font-bold text-brand-900">AgroDairy AI</span>
          </Link>

          <nav className="hidden items-center gap-1 md:flex">
            {NAV_LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.to === '/'}
                className={({ isActive }) =>
                  cn(
                    'rounded-lg px-3.5 py-2 text-sm font-medium transition',
                    isActive ? 'bg-brand-50 text-brand-700' : 'text-brand-600 hover:bg-brand-50',
                  )
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>

          <div className="hidden items-center gap-2 md:flex">
            {user ? (
              <>
                <NotificationBell />
                {user.role === 'CUSTOMER' ? (
                  <Link to="/cart" className="rounded-full p-2 text-brand-600 hover:bg-brand-50" aria-label="Cart">
                    <ShoppingCart className="h-5 w-5" />
                  </Link>
                ) : (
                  <Link to="/admin" className="btn-ghost">
                    <LayoutDashboard className="h-4 w-4" /> Dashboard
                  </Link>
                )}
                <Link
                  to={user.role === 'CUSTOMER' ? '/account' : '/admin'}
                  className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-brand-700 hover:bg-brand-50"
                >
                  <UserCircle className="h-5 w-5" />
                  {user.fullName.split(' ')[0]}
                </Link>
                <button onClick={handleLogout} className="btn-ghost" aria-label="Sign out">
                  <LogOut className="h-4 w-4" />
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn-ghost">
                  Sign in
                </Link>
                <Link to="/register" className="btn-primary">
                  Get started
                </Link>
              </>
            )}
          </div>

          <button
            onClick={() => setMenuOpen((v) => !v)}
            className="rounded-lg p-2 text-brand-700 hover:bg-brand-50 md:hidden"
            aria-label="Toggle menu"
          >
            {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>

        {menuOpen && (
          <div className="border-t border-brand-100 px-4 py-3 md:hidden">
            <nav className="flex flex-col gap-1">
              {NAV_LINKS.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  end={link.to === '/'}
                  onClick={() => setMenuOpen(false)}
                  className="rounded-lg px-3 py-2.5 text-sm font-medium text-brand-700 hover:bg-brand-50"
                >
                  {link.label}
                </NavLink>
              ))}
              {user ? (
                <>
                  {user.role === 'CUSTOMER' ? (
                    <Link to="/cart" onClick={() => setMenuOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-medium text-brand-700 hover:bg-brand-50">
                      Cart
                    </Link>
                  ) : (
                    <Link to="/admin" onClick={() => setMenuOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-medium text-brand-700 hover:bg-brand-50">
                      Dashboard
                    </Link>
                  )}
                  <Link
                    to={user.role === 'CUSTOMER' ? '/account' : '/admin'}
                    onClick={() => setMenuOpen(false)}
                    className="rounded-lg px-3 py-2.5 text-sm font-medium text-brand-700 hover:bg-brand-50"
                  >
                    Account
                  </Link>
                  <button
                    onClick={() => {
                      setMenuOpen(false)
                      handleLogout()
                    }}
                    className="rounded-lg px-3 py-2.5 text-left text-sm font-medium text-red-600 hover:bg-red-50"
                  >
                    Sign out
                  </button>
                </>
              ) : (
                <>
                  <Link to="/login" onClick={() => setMenuOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-medium text-brand-700 hover:bg-brand-50">
                    Sign in
                  </Link>
                  <Link to="/register" onClick={() => setMenuOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-medium text-brand-700 hover:bg-brand-50">
                    Get started
                  </Link>
                </>
              )}
            </nav>
          </div>
        )}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-brand-100 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-8 text-center text-sm text-brand-500 sm:px-6 lg:px-8">
          &copy; {new Date().getFullYear()} AgroDairy AI &middot; Farm-fresh dairy, delivered intelligently.
        </div>
      </footer>
    </div>
  )
}
