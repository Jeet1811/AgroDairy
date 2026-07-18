export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'PACKED'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED'

export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED'

export interface OrderItem {
  id: string
  productId: string
  productName: string
  inventoryBatchId: string | null
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface Order {
  id: string
  userId: string
  status: OrderStatus
  totalAmount: number
  paymentStatus: PaymentStatus
  deliveryAddress: string
  items: OrderItem[]
  createdAt: string
  updatedAt: string
}

export interface CreateOrderRequest {
  deliveryAddress: string
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus
}
