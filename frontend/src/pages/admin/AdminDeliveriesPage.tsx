import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Truck } from 'lucide-react'
import * as deliveriesApi from '@/api/deliveries'
import { PageHeader } from '@/components/ui/PageHeader'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { DeliveryStatusBadge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatDate, todayIso } from '@/lib/format'
import type { DeliveryStatus } from '@/types/delivery'

const DELIVERY_STATUSES: DeliveryStatus[] = ['PENDING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED']

export default function AdminDeliveriesPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [date, setDate] = useState(todayIso())
  const [status, setStatus] = useState<DeliveryStatus | ''>('')
  const [page, setPage] = useState(0)

  const { data: summary } = useQuery({ queryKey: ['deliveries', 'summary', date], queryFn: () => deliveriesApi.getDeliverySummary(date) })
  const { data, isLoading } = useQuery({
    queryKey: ['deliveries', 'admin', { date, status, page }],
    queryFn: () => deliveriesApi.listDeliveries({ date, status: status || undefined, page, size: 15 }),
  })

  const updateStatus = useMutation({
    mutationFn: ({ id, next }: { id: string; next: DeliveryStatus }) => deliveriesApi.updateDeliveryStatus(id, { status: next }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['deliveries'] })
      toast.success('Delivery status updated.')
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Could not update delivery status.')),
  })

  return (
    <div>
      <PageHeader title="Deliveries" description="Plan and track daily delivery runs." />

      <div className="mt-6 flex flex-wrap items-end gap-3">
        <div>
          <label className="label" htmlFor="date">
            Date
          </label>
          <input id="date" type="date" className="input w-auto" value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
        <div>
          <label className="label" htmlFor="statusFilter">
            Status
          </label>
          <select id="statusFilter" className="input w-auto" value={status} onChange={(e) => setStatus(e.target.value as DeliveryStatus | '')}>
            <option value="">All statuses</option>
            {DELIVERY_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </div>
      </div>

      {summary && (
        <div className="card mt-6 p-5">
          <div className="flex items-center gap-2 font-semibold text-brand-900">
            <Truck className="h-4 w-4" /> Summary for {formatDate(date)}
          </div>
          <p className="mt-2 text-sm text-brand-600">
            <span className="font-bold text-brand-900">{summary.totalStops}</span> total stop(s)
          </p>
          {summary.totalItemsByProduct.length > 0 && (
            <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
              {summary.totalItemsByProduct.map((item) => (
                <div key={item.productId} className="rounded-xl bg-brand-50 p-3">
                  <p className="truncate text-xs text-brand-500">{item.name}</p>
                  <p className="text-lg font-bold text-brand-900">{item.totalQuantity}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="card mt-6 overflow-hidden">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState title="No deliveries scheduled" description="Deliveries for this date will appear here." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-brand-100 bg-brand-50/50 text-left text-xs uppercase tracking-wide text-brand-500">
                <tr>
                  <th className="px-4 py-3">Date</th>
                  <th className="hidden px-4 py-3 sm:table-cell">Notes</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Update</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-50">
                {data.items.map((delivery) => (
                  <tr key={delivery.id} className="hover:bg-brand-50/40">
                    <td className="px-4 py-3 font-medium text-brand-900">{formatDate(delivery.deliveryDate)}</td>
                    <td className="hidden px-4 py-3 text-brand-600 sm:table-cell">{delivery.notes ?? '—'}</td>
                    <td className="px-4 py-3">
                      <DeliveryStatusBadge status={delivery.status} />
                    </td>
                    <td className="px-4 py-3">
                      <select
                        className="input ml-auto w-auto !py-1.5 text-xs"
                        value={delivery.status}
                        onChange={(e) => updateStatus.mutate({ id: delivery.id, next: e.target.value as DeliveryStatus })}
                      >
                        {DELIVERY_STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {s.replace(/_/g, ' ')}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
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
