import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/cn'

interface StatCardProps {
  label: string
  value: string
  icon: LucideIcon
  tone?: 'brand' | 'accent' | 'sky' | 'purple'
  hint?: string
}

const TONE_CLASSES: Record<NonNullable<StatCardProps['tone']>, string> = {
  brand: 'bg-brand-50 text-brand-600',
  accent: 'bg-amber-50 text-accent-600',
  sky: 'bg-sky-50 text-sky-600',
  purple: 'bg-purple-50 text-purple-600',
}

export function StatCard({ label, value, icon: Icon, tone = 'brand', hint }: StatCardProps) {
  return (
    <div className="card flex items-start justify-between gap-3 p-5">
      <div className="min-w-0">
        <p className="truncate text-sm font-medium text-brand-500">{label}</p>
        <p className="mt-1.5 text-2xl font-bold tracking-tight text-brand-900">{value}</p>
        {hint && <p className="mt-1 text-xs text-brand-500">{hint}</p>}
      </div>
      <div className={cn('shrink-0 rounded-xl p-2.5', TONE_CLASSES[tone])}>
        <Icon className="h-5 w-5" />
      </div>
    </div>
  )
}
