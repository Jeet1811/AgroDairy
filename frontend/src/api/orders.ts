import { api, unwrap, unwrapPage } from '@/api/client'
import type { CreateOrderRequest, Order, OrderStatus, UpdateOrderStatusRequest } from '@/types/order'

export interface ListOrdersParams {
  status?: OrderStatus
  page?: number
  size?: number
}

export function listOrders(params: ListOrdersParams = {}) {
  return unwrapPage<Order>(api.get('/orders', { params }))
}

export function getOrder(id: string) {
  return unwrap<Order>(api.get(`/orders/${id}`))
}

export function createOrder(request: CreateOrderRequest) {
  return unwrap<Order>(api.post('/orders', request))
}

export function updateOrderStatus(id: string, request: UpdateOrderStatusRequest) {
  return unwrap<Order>(api.patch(`/orders/${id}/status`, request))
}

export function cancelOrder(id: string) {
  return unwrap<Order>(api.post(`/orders/${id}/cancel`))
}
