import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { PackageSearch } from 'lucide-react'
import * as ordersApi from '@/api/orders'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { OrderStatusBadge } from '@/components/ui/Badge'
import { formatCurrency, formatDateTime } from '@/lib/format'

export default function OrdersPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useQuery({
    queryKey: ['orders', { page }],
    queryFn: () => ordersApi.listOrders({ page, size: 10 }),
  })

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold text-brand-900">Your orders</h1>

      <div className="mt-8">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={PackageSearch} title="No orders yet" description="Your placed orders will appear here." />
        ) : (
          <>
            <div className="space-y-3">
              {data.items.map((order) => (
                <Link
                  key={order.id}
                  to={`/orders/${order.id}`}
                  className="card flex flex-col gap-2 p-4 transition hover:shadow-soft-lg sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <p className="font-semibold text-brand-900">Order #{order.id.slice(0, 8)}</p>
                    <p className="text-sm text-brand-500">{formatDateTime(order.createdAt)}</p>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="text-sm text-brand-600">{order.items.length} item(s)</span>
                    <span className="font-semibold text-brand-900">{formatCurrency(order.totalAmount)}</span>
                    <OrderStatusBadge status={order.status} />
                  </div>
                </Link>
              ))}
            </div>
            <div className="mt-6">
              <Pagination meta={data.meta} onPageChange={setPage} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
