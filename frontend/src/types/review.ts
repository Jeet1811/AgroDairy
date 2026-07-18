export interface Review {
  id: string
  productId: string
  userId: string
  rating: number
  comment: string | null
  createdAt: string
}

export interface CreateReviewRequest {
  rating: number
  comment?: string
}
