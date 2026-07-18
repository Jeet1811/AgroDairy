import { useQuery } from '@tanstack/react-query'
import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { AlertTriangle, Boxes, IndianRupee, Milk, PawPrint, Repeat, ShoppingBag, TrendingUp } from 'lucide-react'
import * as analyticsApi from '@/api/analytics'
import { StatCard } from '@/components/ui/StatCard'
import { PageSpinner } from '@/components/ui/Spinner'
import { formatCurrency, formatDate } from '@/lib/format'

export default function AdminDashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ['analytics', 'dashboard'], queryFn: analyticsApi.getDashboard, refetchInterval: 60_000 })

  if (isLoading || !data) return <PageSpinner />

  const revenueTrend = data.revenueTrend.map((point) => ({ ...point, label: formatDate(point.date) }))
  const productionTrend = data.productionTrend.map((point) => ({ ...point, label: formatDate(point.date) }))

  return (
    <div>
      <h1 className="text-2xl font-bold text-brand-900">Dashboard</h1>
      <p className="mt-1 text-brand-600">A live snapshot of your operation.</p>

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Milk today" value={`${data.todayMilkProductionLitres.toFixed(1)} L`} icon={Milk} tone="brand" />
        <StatCard label="Active animals" value={String(data.activeAnimals)} icon={PawPrint} tone="brand" />
        <StatCard label="Active subscriptions" value={String(data.activeSubscriptions)} icon={Repeat} tone="purple" />
        <StatCard label="Products expiring soon" value={String(data.productsExpiringSoon)} icon={AlertTriangle} tone="accent" />
        <StatCard label="Revenue this month" value={formatCurrency(data.monthRevenue)} icon={IndianRupee} tone="brand" />
        <StatCard label="Total orders" value={String(data.totalOrders)} icon={ShoppingBag} tone="sky" />
        <StatCard label="Avg. order value" value={formatCurrency(data.avgOrderValue)} icon={TrendingUp} tone="sky" />
        <StatCard label="Inventory value" value={formatCurrency(data.inventoryValueTotal)} icon={Boxes} tone="accent" />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="card p-5 xl:col-span-2">
          <h2 className="font-semibold text-brand-900">Revenue — last 30 days</h2>
          <div className="mt-4 h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={revenueTrend} margin={{ left: -10 }}>
                <defs>
                  <linearGradient id="revenueFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#3f7e2c" stopOpacity={0.35} />
                    <stop offset="100%" stopColor="#3f7e2c" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e1f1d8" />
                <XAxis dataKey="label" tick={{ fontSize: 11, fill: '#529c39' }} interval={4} />
                <YAxis tick={{ fontSize: 11, fill: '#529c39' }} width={60} />
                <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                <Area type="monotone" dataKey="amount" stroke="#3f7e2c" strokeWidth={2} fill="url(#revenueFill)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card p-5">
          <h2 className="font-semibold text-brand-900">Top-selling products</h2>
          <div className="mt-4 space-y-3">
            {data.topSellingProducts.length === 0 ? (
              <p className="text-sm text-brand-500">No sales recorded yet.</p>
            ) : (
              data.topSellingProducts.map((product, index) => (
                <div key={product.productId} className="flex items-center gap-3">
                  <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-700">
                    {index + 1}
                  </span>
                  <span className="flex-1 truncate text-sm text-brand-800">{product.name}</span>
                  <span className="text-sm font-semibold text-brand-900">{product.unitsSold}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <div className="card mt-6 p-5">
        <h2 className="font-semibold text-brand-900">Milk production — last 30 days</h2>
        <div className="mt-4 h-56">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={productionTrend} margin={{ left: -10 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e1f1d8" />
              <XAxis dataKey="label" tick={{ fontSize: 11, fill: '#529c39' }} interval={4} />
              <YAxis tick={{ fontSize: 11, fill: '#529c39' }} width={60} />
              <Tooltip formatter={(value) => `${value} L`} />
              <Bar dataKey="litres" fill="#74b857" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  )
}
