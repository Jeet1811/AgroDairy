export type DeliveryStatus = 'PENDING' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED'

export interface Delivery {
  id: string
  orderId: string | null
  subscriptionId: string | null
  deliveryDate: string
  status: DeliveryStatus
  deliveredAt: string | null
  notes: string | null
}

export interface UpdateDeliveryStatusRequest {
  status: DeliveryStatus
  notes?: string
}

export interface DeliverySummaryItem {
  productId: string
  name: string
  totalQuantity: number
}

export interface DeliverySummary {
  totalStops: number
  totalItemsByProduct: DeliverySummaryItem[]
}
