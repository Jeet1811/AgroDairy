import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Repeat } from 'lucide-react'
import * as subscriptionsApi from '@/api/subscriptions'
import * as productsApi from '@/api/products'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Modal } from '@/components/ui/Modal'
import { SubscriptionStatusBadge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatDate, todayIso } from '@/lib/format'
import { cn } from '@/lib/cn'
import type { SubscriptionFrequency } from '@/types/subscription'

const WEEKDAYS = [
  { iso: 1, label: 'Mon' },
  { iso: 2, label: 'Tue' },
  { iso: 3, label: 'Wed' },
  { iso: 4, label: 'Thu' },
  { iso: 5, label: 'Fri' },
  { iso: 6, label: 'Sat' },
  { iso: 7, label: 'Sun' },
]

function CreateSubscriptionForm({ onClose }: { onClose: () => void }) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [frequency, setFrequency] = useState<SubscriptionFrequency>('DAILY')
  const [weekdays, setWeekdays] = useState<number[]>([])
  const [deliveryTimeSlot, setDeliveryTimeSlot] = useState('morning')
  const [startDate, setStartDate] = useState(todayIso())
  const [error, setError] = useState<string | null>(null)

  const { data: products } = useQuery({ queryKey: ['products', 'all-active'], queryFn: () => productsApi.listProducts({ active: true, size: 100 }) })

  const create = useMutation({
    mutationFn: () =>
      subscriptionsApi.createSubscription({
        productId,
        quantity,
        frequency,
        weekdays: frequency === 'CUSTOM' ? weekdays.sort().join(',') : undefined,
        deliveryTimeSlot: deliveryTimeSlot || undefined,
        startDate,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] })
      toast.success('Subscription created.')
      onClose()
    },
    onError: (err) => setError(apiErrorMessage(err, 'Could not create subscription.')),
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (!productId) {
      setError('Please choose a product.')
      return
    }
    if (frequency === 'CUSTOM' && weekdays.length === 0) {
      setError('Select at least one delivery day.')
      return
    }
    create.mutate()
  }

  function toggleWeekday(iso: number) {
    setWeekdays((current) => (current.includes(iso) ? current.filter((d) => d !== iso) : [...current, iso]))
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700">{error}</div>}
      <div>
        <label className="label" htmlFor="product">
          Product
        </label>
        <select id="product" className="input" value={productId} onChange={(e) => setProductId(e.target.value)} required>
          <option value="">Select a product…</option>
          {products?.items.map((product) => (
            <option key={product.id} value={product.id}>
              {product.name}
            </option>
          ))}
        </select>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label" htmlFor="quantity">
            Quantity
          </label>
          <input
            id="quantity"
            type="number"
            min={1}
            className="input"
            value={quantity}
            onChange={(e) => setQuantity(Number(e.target.value))}
          />
        </div>
        <div>
          <label className="label" htmlFor="startDate">
            Start date
          </label>
          <input id="startDate" type="date" className="input" value={startDate} min={todayIso()} onChange={(e) => setStartDate(e.target.value)} />
        </div>
      </div>
      <div>
        <label className="label">Frequency</label>
        <div className="flex gap-2">
          {(['DAILY', 'ALTERNATE_DAY', 'CUSTOM'] as SubscriptionFrequency[]).map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => setFrequency(option)}
              className={cn(
                'flex-1 rounded-xl border px-3 py-2 text-sm font-medium transition',
                frequency === option ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-200 text-brand-700 hover:bg-brand-50',
              )}
            >
              {option === 'DAILY' ? 'Daily' : option === 'ALTERNATE_DAY' ? 'Alternate day' : 'Custom days'}
            </button>
          ))}
        </div>
      </div>
      {frequency === 'CUSTOM' && (
        <div>
          <label className="label">Delivery days</label>
          <div className="flex flex-wrap gap-2">
            {WEEKDAYS.map((day) => (
              <button
                key={day.iso}
                type="button"
                onClick={() => toggleWeekday(day.iso)}
                className={cn(
                  'rounded-full border px-3 py-1.5 text-sm font-medium transition',
                  weekdays.includes(day.iso) ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-200 text-brand-700 hover:bg-brand-50',
                )}
              >
                {day.label}
              </button>
            ))}
          </div>
        </div>
      )}
      <div>
        <label className="label" htmlFor="timeSlot">
          Preferred delivery time
        </label>
        <select id="timeSlot" className="input" value={deliveryTimeSlot} onChange={(e) => setDeliveryTimeSlot(e.target.value)}>
          <option value="morning">Morning</option>
          <option value="evening">Evening</option>
        </select>
      </div>
      <button type="submit" disabled={create.isPending} className="btn-primary w-full">
        {create.isPending ? 'Creating…' : 'Create subscription'}
      </button>
    </form>
  )
}

export default function SubscriptionsPage() {
  const [createOpen, setCreateOpen] = useState(false)
  const { data, isLoading } = useQuery({ queryKey: ['subscriptions'], queryFn: () => subscriptionsApi.listSubscriptions({ size: 50 }) })

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-brand-900">Your subscriptions</h1>
        <button onClick={() => setCreateOpen(true)} className="btn-primary">
          <Plus className="h-4 w-4" /> New
        </button>
      </div>

      <div className="mt-8">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            icon={Repeat}
            title="No subscriptions yet"
            description="Set up a recurring delivery so you never run out."
            action={
              <button onClick={() => setCreateOpen(true)} className="btn-primary">
                Create a subscription
              </button>
            }
          />
        ) : (
          <div className="space-y-3">
            {data.items.map((subscription) => (
              <Link
                key={subscription.id}
                to={`/subscriptions/${subscription.id}`}
                className="card flex flex-col gap-2 p-4 transition hover:shadow-soft-lg sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <p className="font-semibold text-brand-900">{subscription.productName}</p>
                  <p className="text-sm text-brand-500">
                    Qty {subscription.quantity} &middot; {subscription.frequency.replace('_', ' ').toLowerCase()} &middot; since{' '}
                    {formatDate(subscription.startDate)}
                  </p>
                </div>
                <SubscriptionStatusBadge status={subscription.status} />
              </Link>
            ))}
          </div>
        )}
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="New subscription">
        <CreateSubscriptionForm onClose={() => setCreateOpen(false)} />
      </Modal>
    </div>
  )
}
