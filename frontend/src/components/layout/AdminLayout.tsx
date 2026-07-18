import {
  LayoutDashboard,
  PawPrint,
  Package,
  Boxes,
  ClipboardList,
  Repeat,
  Truck,
  AlertTriangle,
  LineChart,
  Users,
} from 'lucide-react'
import { DashboardShell, type NavItem } from '@/components/layout/DashboardShell'
import { useAuth } from '@/context/AuthContext'

const BASE_NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/admin', icon: LayoutDashboard, end: true },
  { label: 'Animals', to: '/admin/animals', icon: PawPrint },
  { label: 'Products', to: '/admin/products', icon: Package },
  { label: 'Inventory', to: '/admin/inventory', icon: Boxes },
  { label: 'Orders', to: '/admin/orders', icon: ClipboardList },
  { label: 'Subscriptions', to: '/admin/subscriptions', icon: Repeat },
  { label: 'Deliveries', to: '/admin/deliveries', icon: Truck },
  { label: 'Anomalies', to: '/admin/anomalies', icon: AlertTriangle },
  { label: 'Forecast', to: '/admin/forecast', icon: LineChart },
]

export function AdminLayout() {
  const { user } = useAuth()
  const navItems =
    user?.role === 'ADMIN' ? [...BASE_NAV_ITEMS, { label: 'Users', to: '/admin/users', icon: Users }] : BASE_NAV_ITEMS

  return <DashboardShell navItems={navItems} brandLabel="Operations" />
}
