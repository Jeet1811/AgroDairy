import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronDown, ChevronUp } from 'lucide-react'
import * as ordersApi from '@/api/orders'
import { PageHeader } from '@/components/ui/PageHeader'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { OrderStatusBadge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatCurrency, formatDateTime } from '@/lib/format'
import type { OrderStatus } from '@/types/order'

const ORDER_STATUSES: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PROCESSING', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'REFUNDED']

const NEXT_STATUS: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['PACKED'],
  PACKED: ['OUT_FOR_DELIVERY'],
  OUT_FOR_DELIVERY: ['DELIVERED'],
  DELIVERED: ['REFUNDED'],
  CANCELLED: ['REFUNDED'],
  REFUNDED: [],
}

export default function AdminOrdersPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [page, setPage] = useState(0)
  const [expandedId, setExpandedId] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['orders', 'admin', { status, page }],
    queryFn: () => ordersApi.listOrders({ status: status || undefined, page, size: 15 }),
  })

  const updateStatus = useMutation({
    mutationFn: ({ id, next }: { id: string; next: OrderStatus }) => ordersApi.updateOrderStatus(id, { status: next }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      toast.success('Order status updated.')
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Could not update order status.')),
  })

  return (
    <div>
      <PageHeader title="Orders" description="Track and progress customer orders." />

      <div className="mt-6">
        <select
          className="input w-auto"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as OrderStatus | '')
            setPage(0)
          }}
        >
          <option value="">All statuses</option>
          {ORDER_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s.replace(/_/g, ' ')}
            </option>
          ))}
        </select>
      </div>

      <div className="card mt-6 overflow-hidden">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState title="No orders found" />
        ) : (
          <div className="divide-y divide-brand-50">
            {data.items.map((order) => {
              const expanded = expandedId === order.id
              const nextOptions = NEXT_STATUS[order.status]
              return (
                <div key={order.id}>
                  <button
                    onClick={() => setExpandedId(expanded ? null : order.id)}
                    className="flex w-full flex-wrap items-center justify-between gap-3 px-4 py-3.5 text-left hover:bg-brand-50/40"
                  >
                    <div className="min-w-0">
                      <p className="font-semibold text-brand-900">#{order.id.slice(0, 8)}</p>
                      <p className="text-xs text-brand-500">{formatDateTime(order.createdAt)}</p>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-semibold text-brand-900">{formatCurrency(order.totalAmount)}</span>
                      <OrderStatusBadge status={order.status} />
                      {expanded ? <ChevronUp className="h-4 w-4 text-brand-400" /> : <ChevronDown className="h-4 w-4 text-brand-400" />}
                    </div>
                  </button>
                  {expanded && (
                    <div className="bg-brand-50/30 px-4 pb-4">
                      <div className="space-y-1.5 py-3 text-sm">
                        {order.items.map((item) => (
                          <div key={item.id} className="flex justify-between">
                            <span className="text-brand-700">
                              {item.productName} &times;{item.quantity}
                            </span>
                            <span className="font-medium text-brand-900">{formatCurrency(item.subtotal)}</span>
                          </div>
                        ))}
                      </div>
                      <p className="text-xs text-brand-500">Delivery to: {order.deliveryAddress}</p>
                      {nextOptions.length > 0 && (
                        <div className="mt-3 flex flex-wrap gap-2">
                          {nextOptions.map((next) => (
                            <button
                              key={next}
                              onClick={() => updateStatus.mutate({ id: order.id, next })}
                              disabled={updateStatus.isPending}
                              className="btn-secondary !px-3 !py-1.5 text-xs"
                            >
                              Mark {next.replace(/_/g, ' ').toLowerCase()}
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
        {data && (
          <div className="px-4 pb-4">
            <Pagination meta={data.meta} onPageChange={setPage} />
          </div>
        )}
      </div>
    </div>
  )
}
