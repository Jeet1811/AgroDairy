export type NotificationType = 'ANOMALY_ALERT' | 'INVENTORY_EXPIRY' | 'ORDER_STATUS_CHANGE' | 'DELIVERY_GENERATED' | 'WELCOME'

export interface Notification {
  id: string
  type: NotificationType
  message: string
  isRead: boolean
  createdAt: string
}
