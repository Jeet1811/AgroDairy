export type CategoryKind = 'DAIRY' | 'AGRICULTURE'

export interface Category {
  id: string
  name: string
  kind: CategoryKind
}

export interface Product {
  id: string
  categoryId: string
  name: string
  description: string | null
  price: number
  unit: string
  shelfLifeDays: number | null
  imageUrl: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateProductRequest {
  categoryId: string
  name: string
  description?: string
  price: number
  unit: string
  shelfLifeDays?: number
  imageUrl?: string
}

export type UpdateProductRequest = Partial<CreateProductRequest>

export interface CreateCategoryRequest {
  name: string
  kind: CategoryKind
}
