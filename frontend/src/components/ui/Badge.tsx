import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { titleCase } from '@/lib/format'

export type BadgeTone = 'green' | 'amber' | 'red' | 'blue' | 'gray' | 'purple'

const TONE_CLASSES: Record<BadgeTone, string> = {
  green: 'bg-brand-100 text-brand-800',
  amber: 'bg-amber-100 text-amber-800',
  red: 'bg-red-100 text-red-700',
  blue: 'bg-sky-100 text-sky-800',
  gray: 'bg-gray-100 text-gray-700',
  purple: 'bg-purple-100 text-purple-800',
}

export function Badge({ tone = 'gray', children }: { tone?: BadgeTone; children: ReactNode }) {
  return <span className={cn('badge', TONE_CLASSES[tone])}>{children}</span>
}

const ORDER_STATUS_TONES: Record<string, BadgeTone> = {
  PENDING: 'amber',
  CONFIRMED: 'blue',
  PROCESSING: 'blue',
  PACKED: 'purple',
  OUT_FOR_DELIVERY: 'purple',
  DELIVERED: 'green',
  CANCELLED: 'red',
  REFUNDED: 'gray',
}

const SUBSCRIPTION_STATUS_TONES: Record<string, BadgeTone> = {
  ACTIVE: 'green',
  PAUSED: 'amber',
  CANCELLED: 'red',
  EXPIRED: 'gray',
}

const DELIVERY_STATUS_TONES: Record<string, BadgeTone> = {
  PENDING: 'amber',
  OUT_FOR_DELIVERY: 'purple',
  DELIVERED: 'green',
  FAILED: 'red',
}

const BATCH_STATUS_TONES: Record<string, BadgeTone> = {
  ACTIVE: 'green',
  EXPIRED: 'red',
  SOLD_OUT: 'gray',
  DISCARDED: 'gray',
}

const SEVERITY_TONES: Record<string, BadgeTone> = {
  LOW: 'blue',
  MEDIUM: 'amber',
  HIGH: 'red',
}

const ANIMAL_STATUS_TONES: Record<string, BadgeTone> = {
  ACTIVE: 'green',
  MILKING: 'green',
  PREGNANT: 'purple',
  DRY_PERIOD: 'amber',
  UNDER_OBSERVATION: 'amber',
  SOLD: 'gray',
  DECEASED: 'gray',
}

function StatusBadge({ status, tones }: { status: string; tones: Record<string, BadgeTone> }) {
  return <Badge tone={tones[status] ?? 'gray'}>{titleCase(status)}</Badge>
}

export const OrderStatusBadge = ({ status }: { status: string }) => <StatusBadge status={status} tones={ORDER_STATUS_TONES} />
export const SubscriptionStatusBadge = ({ status }: { status: string }) => (
  <StatusBadge status={status} tones={SUBSCRIPTION_STATUS_TONES} />
)
export const DeliveryStatusBadge = ({ status }: { status: string }) => <StatusBadge status={status} tones={DELIVERY_STATUS_TONES} />
export const BatchStatusBadge = ({ status }: { status: string }) => <StatusBadge status={status} tones={BATCH_STATUS_TONES} />
export const SeverityBadge = ({ status }: { status: string }) => <StatusBadge status={status} tones={SEVERITY_TONES} />
export const AnimalStatusBadge = ({ status }: { status: string }) => <StatusBadge status={status} tones={ANIMAL_STATUS_TONES} />
