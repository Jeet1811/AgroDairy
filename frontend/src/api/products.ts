import { api, unwrap, unwrapPage } from '@/api/client'
import type { Category, CategoryKind, CreateCategoryRequest, CreateProductRequest, Product, UpdateProductRequest } from '@/types/product'

export interface ListProductsParams {
  categoryId?: string
  kind?: CategoryKind
  active?: boolean
  search?: string
  page?: number
  size?: number
}

export function listProducts(params: ListProductsParams = {}) {
  return unwrapPage<Product>(api.get('/products', { params }))
}

export function getProduct(id: string) {
  return unwrap<Product>(api.get(`/products/${id}`))
}

export function createProduct(request: CreateProductRequest) {
  return unwrap<Product>(api.post('/products', request))
}

export function updateProduct(id: string, request: UpdateProductRequest) {
  return unwrap<Product>(api.patch(`/products/${id}`, request))
}

export function deactivateProduct(id: string) {
  return api.delete(`/products/${id}`)
}

export function listCategories() {
  return unwrap<Category[]>(api.get('/categories'))
}

export function createCategory(request: CreateCategoryRequest) {
  return unwrap<Category>(api.post('/categories', request))
}
