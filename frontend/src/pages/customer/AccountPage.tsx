import { useQuery } from '@tanstack/react-query'
import { Truck, UserCircle } from 'lucide-react'
import * as deliveriesApi from '@/api/deliveries'
import { useAuth } from '@/context/AuthContext'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { DeliveryStatusBadge } from '@/components/ui/Badge'
import { formatDate } from '@/lib/format'

export default function AccountPage() {
  const { user } = useAuth()
  const { data, isLoading } = useQuery({ queryKey: ['deliveries', 'mine'], queryFn: () => deliveriesApi.listMyDeliveries({ size: 10 }) })

  if (!user) return null

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold text-brand-900">Account</h1>

      <div className="card mt-6 flex items-center gap-4 p-5">
        <div className="rounded-full bg-brand-100 p-3">
          <UserCircle className="h-8 w-8 text-brand-600" />
        </div>
        <div>
          <p className="font-semibold text-brand-900">{user.fullName}</p>
          <p className="text-sm text-brand-500">{user.email}</p>
          {user.phone && <p className="text-sm text-brand-500">{user.phone}</p>}
        </div>
      </div>

      <h2 className="mt-8 flex items-center gap-2 text-lg font-semibold text-brand-900">
        <Truck className="h-5 w-5" /> Upcoming deliveries
      </h2>
      <div className="mt-4">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState title="No deliveries scheduled" description="Deliveries from your active subscriptions will show up here." />
        ) : (
          <div className="space-y-3">
            {data.items.map((delivery) => (
              <div key={delivery.id} className="card flex items-center justify-between p-4">
                <div>
                  <p className="font-medium text-brand-900">{formatDate(delivery.deliveryDate)}</p>
                  {delivery.notes && <p className="text-sm text-brand-500">{delivery.notes}</p>}
                </div>
                <DeliveryStatusBadge status={delivery.status} />
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
