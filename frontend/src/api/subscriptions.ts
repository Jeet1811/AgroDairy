import { api, unwrap, unwrapPage } from '@/api/client'
import type {
  CreateSubscriptionRequest,
  SkipSubscriptionRequest,
  Subscription,
  SubscriptionStatus,
  UpdateSubscriptionRequest,
} from '@/types/subscription'

export interface ListSubscriptionsParams {
  status?: SubscriptionStatus
  page?: number
  size?: number
}

export function listSubscriptions(params: ListSubscriptionsParams = {}) {
  return unwrapPage<Subscription>(api.get('/subscriptions', { params }))
}

export function getSubscription(id: string) {
  return unwrap<Subscription>(api.get(`/subscriptions/${id}`))
}

export function createSubscription(request: CreateSubscriptionRequest) {
  return unwrap<Subscription>(api.post('/subscriptions', request))
}

export function updateSubscription(id: string, request: UpdateSubscriptionRequest) {
  return unwrap<Subscription>(api.patch(`/subscriptions/${id}`, request))
}

export function pauseSubscription(id: string) {
  return unwrap<Subscription>(api.post(`/subscriptions/${id}/pause`))
}

export function resumeSubscription(id: string) {
  return unwrap<Subscription>(api.post(`/subscriptions/${id}/resume`))
}

export function cancelSubscription(id: string) {
  return unwrap<Subscription>(api.post(`/subscriptions/${id}/cancel`))
}

export function skipSubscription(id: string, request: SkipSubscriptionRequest) {
  return unwrap<Subscription>(api.post(`/subscriptions/${id}/skip`, request))
}
