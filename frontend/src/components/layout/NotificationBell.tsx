import { useEffect, useRef, useState } from 'react'
import { Bell } from 'lucide-react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import * as notificationsApi from '@/api/notifications'
import { formatDateTime } from '@/lib/format'
import { cn } from '@/lib/cn'
import { Spinner } from '@/components/ui/Spinner'

export function NotificationBell() {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['notifications', { size: 8 }],
    queryFn: () => notificationsApi.listNotifications({ size: 8 }),
    refetchInterval: 60_000,
  })

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  const unreadCount = data?.items.filter((n) => !n.isRead).length ?? 0

  async function markRead(id: string) {
    await notificationsApi.markNotificationRead(id)
    queryClient.invalidateQueries({ queryKey: ['notifications'] })
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={() => setOpen((v) => !v)}
        className="relative rounded-full p-2 text-brand-600 hover:bg-brand-50"
        aria-label="Notifications"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-accent-500 px-1 text-[10px] font-bold text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>
      {open && (
        <div className="absolute right-0 z-40 mt-2 w-80 max-w-[90vw] rounded-2xl border border-brand-100 bg-white shadow-soft-lg">
          <div className="border-b border-brand-100 px-4 py-3">
            <p className="text-sm font-semibold text-brand-900">Notifications</p>
          </div>
          <div className="max-h-96 overflow-y-auto">
            {isLoading ? (
              <div className="flex justify-center py-8">
                <Spinner className="h-5 w-5" />
              </div>
            ) : !data || data.items.length === 0 ? (
              <p className="px-4 py-8 text-center text-sm text-brand-500">You're all caught up.</p>
            ) : (
              data.items.map((notification) => (
                <button
                  key={notification.id}
                  onClick={() => !notification.isRead && markRead(notification.id)}
                  className={cn(
                    'block w-full border-b border-brand-50 px-4 py-3 text-left text-sm last:border-0 hover:bg-brand-50',
                    !notification.isRead && 'bg-brand-50/60',
                  )}
                >
                  <p className={cn('text-brand-800', !notification.isRead && 'font-semibold text-brand-900')}>
                    {notification.message}
                  </p>
                  <p className="mt-1 text-xs text-brand-400">{formatDateTime(notification.createdAt)}</p>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}
