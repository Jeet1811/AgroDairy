import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { PageMeta } from '@/types/api'

interface PaginationProps {
  meta: PageMeta
  onPageChange: (page: number) => void
}

export function Pagination({ meta, onPageChange }: PaginationProps) {
  if (meta.totalPages <= 1) return null

  const isFirst = meta.page <= 0
  const isLast = meta.page >= meta.totalPages - 1

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-brand-100 px-1 pt-4 text-sm">
      <p className="text-brand-600">
        Page <span className="font-semibold text-brand-900">{meta.page + 1}</span> of{' '}
        <span className="font-semibold text-brand-900">{meta.totalPages}</span>
        <span className="hidden sm:inline"> &middot; {meta.totalElements} total</span>
      </p>
      <div className="flex gap-2">
        <button
          className="btn-secondary !px-3 !py-2"
          disabled={isFirst}
          onClick={() => onPageChange(meta.page - 1)}
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <button
          className="btn-secondary !px-3 !py-2"
          disabled={isLast}
          onClick={() => onPageChange(meta.page + 1)}
          aria-label="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
