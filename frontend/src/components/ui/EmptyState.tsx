import type { LucideIcon } from 'lucide-react'
import { Inbox } from 'lucide-react'
import type { ReactNode } from 'react'

interface EmptyStateProps {
  icon?: LucideIcon
  title: string
  description?: string
  action?: ReactNode
}

export function EmptyState({ icon: Icon = Inbox, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-brand-200 bg-white/60 px-6 py-14 text-center">
      <div className="rounded-full bg-brand-50 p-3">
        <Icon className="h-6 w-6 text-brand-400" />
      </div>
      <div>
        <p className="font-semibold text-brand-900">{title}</p>
        {description && <p className="mt-1 max-w-sm text-sm text-brand-600">{description}</p>}
      </div>
      {action}
    </div>
  )
}
