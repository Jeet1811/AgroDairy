export interface PageMeta {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ApiEnvelope<T> {
  success: boolean
  data: T
  meta: PageMeta | null
}

export interface ApiErrorBody {
  success: false
  error: {
    code: string
    message: string
    details?: unknown
  }
}

export interface Page<T> {
  items: T[]
  meta: PageMeta
}
