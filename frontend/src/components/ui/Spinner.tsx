import { Loader2 } from 'lucide-react'
import { cn } from '@/lib/cn'

export function Spinner({ className }: { className?: string }) {
  return <Loader2 className={cn('animate-spin text-brand-500', className)} />
}

export function PageSpinner() {
  return (
    <div className="flex min-h-[50vh] w-full items-center justify-center">
      <Spinner className="h-8 w-8" />
    </div>
  )
}

export function InlineSpinner() {
  return (
    <div className="flex items-center justify-center py-10">
      <Spinner className="h-6 w-6" />
    </div>
  )
}
