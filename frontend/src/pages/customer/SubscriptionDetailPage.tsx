import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarOff, ChevronLeft, Pause, Play, XCircle } from 'lucide-react'
import * as subscriptionsApi from '@/api/subscriptions'
import { PageSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { SubscriptionStatusBadge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatDate, todayIso } from '@/lib/format'

export default function SubscriptionDetailPage() {
  const { id } = useParams<{ id: string }>()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [skipDate, setSkipDate] = useState(todayIso())

  const { data: subscription, isLoading } = useQuery({
    queryKey: ['subscriptions', id],
    queryFn: () => subscriptionsApi.getSubscription(id!),
    enabled: !!id,
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['subscriptions'] })
  }

  const pause = useMutation({
    mutationFn: () => subscriptionsApi.pauseSubscription(id!),
    onSuccess: () => {
      invalidate()
      toast.success('Subscription paused.')
    },
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not pause subscription.')),
  })
  const resume = useMutation({
    mutationFn: () => subscriptionsApi.resumeSubscription(id!),
    onSuccess: () => {
      invalidate()
      toast.success('Subscription resumed.')
    },
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not resume subscription.')),
  })
  const cancel = useMutation({
    mutationFn: () => subscriptionsApi.cancelSubscription(id!),
    onSuccess: () => {
      invalidate()
      toast.success('Subscription cancelled.')
    },
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not cancel subscription.')),
  })
  const skip = useMutation({
    mutationFn: () => subscriptionsApi.skipSubscription(id!, { skipDate }),
    onSuccess: () => {
      invalidate()
      toast.success(`Delivery on ${formatDate(skipDate)} will be skipped.`)
    },
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not skip that date.')),
  })

  if (isLoading) return <PageSpinner />
  if (!subscription) return <EmptyState title="Subscription not found" />

  const isActive = subscription.status === 'ACTIVE'
  const isPaused = subscription.status === 'PAUSED'
  const isCancellable = isActive || isPaused

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6 lg:px-8">
      <Link to="/subscriptions" className="mb-6 flex items-center gap-1 text-sm font-medium text-brand-600 hover:text-brand-800">
        <ChevronLeft className="h-4 w-4" /> Back to subscriptions
      </Link>

      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-brand-900">{subscription.productName}</h1>
        <SubscriptionStatusBadge status={subscription.status} />
      </div>

      <div className="card mt-6 divide-y divide-brand-100 p-5">
        <div className="flex justify-between py-2 text-sm">
          <span className="text-brand-500">Quantity</span>
          <span className="font-medium text-brand-900">{subscription.quantity}</span>
        </div>
        <div className="flex justify-between py-2 text-sm">
          <span className="text-brand-500">Frequency</span>
          <span className="font-medium text-brand-900">{subscription.frequency.replace('_', ' ')}</span>
        </div>
        {subscription.weekdays && (
          <div className="flex justify-between py-2 text-sm">
            <span className="text-brand-500">Delivery days</span>
            <span className="font-medium text-brand-900">{subscription.weekdays}</span>
          </div>
        )}
        {subscription.deliveryTimeSlot && (
          <div className="flex justify-between py-2 text-sm">
            <span className="text-brand-500">Preferred time</span>
            <span className="font-medium capitalize text-brand-900">{subscription.deliveryTimeSlot}</span>
          </div>
        )}
        <div className="flex justify-between py-2 text-sm">
          <span className="text-brand-500">Start date</span>
          <span className="font-medium text-brand-900">{formatDate(subscription.startDate)}</span>
        </div>
        {subscription.endDate && (
          <div className="flex justify-between py-2 text-sm">
            <span className="text-brand-500">End date</span>
            <span className="font-medium text-brand-900">{formatDate(subscription.endDate)}</span>
          </div>
        )}
      </div>

      {isCancellable && (
        <div className="mt-6 flex flex-wrap gap-3">
          {isActive && (
            <button onClick={() => pause.mutate()} disabled={pause.isPending} className="btn-secondary">
              <Pause className="h-4 w-4" /> Pause
            </button>
          )}
          {isPaused && (
            <button onClick={() => resume.mutate()} disabled={resume.isPending} className="btn-secondary">
              <Play className="h-4 w-4" /> Resume
            </button>
          )}
          <button onClick={() => cancel.mutate()} disabled={cancel.isPending} className="btn-danger">
            <XCircle className="h-4 w-4" /> Cancel subscription
          </button>
        </div>
      )}

      {isActive && (
        <div className="card mt-6 p-5">
          <h2 className="flex items-center gap-2 font-semibold text-brand-900">
            <CalendarOff className="h-4 w-4" /> Skip a delivery
          </h2>
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <input type="date" className="input max-w-xs" min={todayIso()} value={skipDate} onChange={(e) => setSkipDate(e.target.value)} />
            <button onClick={() => skip.mutate()} disabled={skip.isPending} className="btn-secondary">
              {skip.isPending ? 'Skipping…' : 'Skip this date'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
