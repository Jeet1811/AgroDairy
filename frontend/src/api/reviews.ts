import { api, unwrap, unwrapPage } from '@/api/client'
import type { CreateReviewRequest, Review } from '@/types/review'

export function listReviews(productId: string, params: { page?: number; size?: number } = {}) {
  return unwrapPage<Review>(api.get(`/products/${productId}/reviews`, { params }))
}

export function createReview(productId: string, request: CreateReviewRequest) {
  return unwrap<Review>(api.post(`/products/${productId}/reviews`, request))
}
