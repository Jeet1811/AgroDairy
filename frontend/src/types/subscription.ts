export type SubscriptionFrequency = 'DAILY' | 'ALTERNATE_DAY' | 'CUSTOM'
export type SubscriptionStatus = 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'EXPIRED'

export interface Subscription {
  id: string
  userId: string
  productId: string
  productName: string
  quantity: number
  frequency: SubscriptionFrequency
  weekdays: string | null
  deliveryTimeSlot: string | null
  startDate: string
  endDate: string | null
  status: SubscriptionStatus
  createdAt: string
  updatedAt: string
}

export interface CreateSubscriptionRequest {
  productId: string
  quantity: number
  frequency: SubscriptionFrequency
  weekdays?: string
  deliveryTimeSlot?: string
  startDate: string
  endDate?: string
}

export interface UpdateSubscriptionRequest {
  quantity?: number
  weekdays?: string
  deliveryTimeSlot?: string
  endDate?: string
}

export interface SkipSubscriptionRequest {
  skipDate: string
}
