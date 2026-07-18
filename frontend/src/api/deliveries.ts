import { api, unwrap, unwrapPage } from '@/api/client'
import type { Delivery, DeliveryStatus, DeliverySummary, UpdateDeliveryStatusRequest } from '@/types/delivery'

export interface ListDeliveriesParams {
  date?: string
  status?: DeliveryStatus
  page?: number
  size?: number
}

export function listDeliveries(params: ListDeliveriesParams = {}) {
  return unwrapPage<Delivery>(api.get('/deliveries', { params }))
}

export function listMyDeliveries(params: { page?: number; size?: number } = {}) {
  return unwrapPage<Delivery>(api.get('/deliveries/mine', { params }))
}

export function updateDeliveryStatus(id: string, request: UpdateDeliveryStatusRequest) {
  return unwrap<Delivery>(api.patch(`/deliveries/${id}/status`, request))
}

export function getDeliverySummary(date: string) {
  return unwrap<DeliverySummary>(api.get('/deliveries/summary', { params: { date } }))
}
