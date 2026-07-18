import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, XCircle } from 'lucide-react'
import * as ordersApi from '@/api/orders'
import { PageSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { OrderStatusBadge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatCurrency, formatDateTime } from '@/lib/format'

const STATUS_STEPS = ['PENDING', 'CONFIRMED', 'PROCESSING', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED'] as const

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const toast = useToast()
  const queryClient = useQueryClient()

  const { data: order, isLoading } = useQuery({
    queryKey: ['orders', id],
    queryFn: () => ordersApi.getOrder(id!),
    enabled: !!id,
  })

  const cancelOrder = useMutation({
    mutationFn: () => ordersApi.cancelOrder(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      toast.success('Order cancelled.')
    },
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not cancel this order.')),
  })

  if (isLoading) return <PageSpinner />
  if (!order) return <EmptyState title="Order not found" />

  const currentStepIndex = STATUS_STEPS.indexOf(order.status as (typeof STATUS_STEPS)[number])
  const isTerminalNonDelivered = order.status === 'CANCELLED' || order.status === 'REFUNDED'

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <Link to="/orders" className="mb-6 flex items-center gap-1 text-sm font-medium text-brand-600 hover:text-brand-800">
        <ChevronLeft className="h-4 w-4" /> Back to orders
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-brand-900">Order #{order.id.slice(0, 8)}</h1>
          <p className="mt-1 text-sm text-brand-500">Placed {formatDateTime(order.createdAt)}</p>
        </div>
        <OrderStatusBadge status={order.status} />
      </div>

      {!isTerminalNonDelivered && (
        <div className="card mt-6 overflow-x-auto p-5">
          <div className="flex min-w-max items-center">
            {STATUS_STEPS.map((step, index) => (
              <div key={step} className="flex flex-1 items-center last:flex-none">
                <div className="flex flex-col items-center gap-1.5">
                  <div
                    className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                      index <= currentStepIndex ? 'bg-brand-600 text-white' : 'bg-brand-100 text-brand-400'
                    }`}
                  >
                    {index + 1}
                  </div>
                  <span className={`whitespace-nowrap text-[11px] font-medium ${index <= currentStepIndex ? 'text-brand-800' : 'text-brand-400'}`}>
                    {step.replace(/_/g, ' ')}
                  </span>
                </div>
                {index < STATUS_STEPS.length - 1 && (
                  <div className={`mx-2 h-0.5 flex-1 ${index < currentStepIndex ? 'bg-brand-600' : 'bg-brand-100'}`} />
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="card mt-6 p-5">
        <h2 className="font-semibold text-brand-900">Items</h2>
        <div className="mt-4 space-y-3">
          {order.items.map((item) => (
            <div key={item.id} className="flex justify-between text-sm">
              <span className="text-brand-700">
                {item.productName} <span className="text-brand-400">&times;{item.quantity}</span>
              </span>
              <span className="font-medium text-brand-900">{formatCurrency(item.subtotal)}</span>
            </div>
          ))}
        </div>
        <div className="mt-4 flex justify-between border-t border-brand-100 pt-3 font-bold text-brand-900">
          <span>Total</span>
          <span>{formatCurrency(order.totalAmount)}</span>
        </div>
      </div>

      <div className="card mt-6 p-5">
        <h2 className="font-semibold text-brand-900">Delivery address</h2>
        <p className="mt-2 whitespace-pre-line text-sm text-brand-600">{order.deliveryAddress}</p>
      </div>

      {order.status === 'PENDING' && (
        <button onClick={() => cancelOrder.mutate()} disabled={cancelOrder.isPending} className="btn-danger mt-6">
          <XCircle className="h-4 w-4" />
          {cancelOrder.isPending ? 'Cancelling…' : 'Cancel order'}
        </button>
      )}
    </div>
  )
}
