import { ShoppingCart, PackageSearch, Repeat, UserCircle, Store } from 'lucide-react'
import { DashboardShell, type NavItem } from '@/components/layout/DashboardShell'

const NAV_ITEMS: NavItem[] = [
  { label: 'Shop', to: '/products', icon: Store },
  { label: 'Cart', to: '/cart', icon: ShoppingCart },
  { label: 'Orders', to: '/orders', icon: PackageSearch },
  { label: 'Subscriptions', to: '/subscriptions', icon: Repeat },
  { label: 'Account', to: '/account', icon: UserCircle },
]

export function CustomerLayout() {
  return <DashboardShell navItems={NAV_ITEMS} brandLabel="Customer" />
}
