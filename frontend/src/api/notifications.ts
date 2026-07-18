import { api, unwrap, unwrapPage } from '@/api/client'
import type { Notification } from '@/types/notification'

export function listNotifications(params: { isRead?: boolean; page?: number; size?: number } = {}) {
  return unwrapPage<Notification>(api.get('/notifications', { params }))
}

export function markNotificationRead(id: string) {
  return unwrap<Notification>(api.patch(`/notifications/${id}/read`))
}
