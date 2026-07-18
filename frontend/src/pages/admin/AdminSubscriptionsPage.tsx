import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import * as subscriptionsApi from '@/api/subscriptions'
import { PageHeader } from '@/components/ui/PageHeader'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { SubscriptionStatusBadge } from '@/components/ui/Badge'
import { formatDate, titleCase } from '@/lib/format'
import type { SubscriptionStatus } from '@/types/subscription'

const SUBSCRIPTION_STATUSES: SubscriptionStatus[] = ['ACTIVE', 'PAUSED', 'CANCELLED', 'EXPIRED']

export default function AdminSubscriptionsPage() {
  const [status, setStatus] = useState<SubscriptionStatus | ''>('')
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['subscriptions', 'admin', { status, page }],
    queryFn: () => subscriptionsApi.listSubscriptions({ status: status || undefined, page, size: 15 }),
  })

  return (
    <div>
      <PageHeader title="Subscriptions" description="Monitor recurring deliveries across all customers." />

      <div className="mt-6">
        <select
          className="input w-auto"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as SubscriptionStatus | '')
            setPage(0)
          }}
        >
          <option value="">All statuses</option>
          {SUBSCRIPTION_STATUSES.map((s) => (
            <option key={s} value={s}>
              {titleCase(s)}
            </option>
          ))}
        </select>
      </div>

      <div className="card mt-6 overflow-hidden">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState title="No subscriptions found" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-brand-100 bg-brand-50/50 text-left text-xs uppercase tracking-wide text-brand-500">
                <tr>
                  <th className="px-4 py-3">Product</th>
                  <th className="px-4 py-3">Qty</th>
                  <th className="hidden px-4 py-3 sm:table-cell">Frequency</th>
                  <th className="hidden px-4 py-3 md:table-cell">Started</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-50">
                {data.items.map((subscription) => (
                  <tr key={subscription.id} className="hover:bg-brand-50/40">
                    <td className="px-4 py-3 font-medium text-brand-900">{subscription.productName}</td>
                    <td className="px-4 py-3 text-brand-700">{subscription.quantity}</td>
                    <td className="hidden px-4 py-3 text-brand-600 sm:table-cell">{titleCase(subscription.frequency)}</td>
                    <td className="hidden px-4 py-3 text-brand-500 md:table-cell">{formatDate(subscription.startDate)}</td>
                    <td className="px-4 py-3">
                      <SubscriptionStatusBadge status={subscription.status} />
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
