import { api, unwrap } from '@/api/client'
import type { AddCartItemRequest, Cart, UpdateCartItemRequest } from '@/types/cart'

export function getCart() {
  return unwrap<Cart>(api.get('/cart'))
}

export function addCartItem(request: AddCartItemRequest) {
  return unwrap<Cart>(api.post('/cart/items', request))
}

export function updateCartItem(productId: string, request: UpdateCartItemRequest) {
  return unwrap<Cart>(api.patch(`/cart/items/${productId}`, request))
}

export function removeCartItem(productId: string) {
  return unwrap<Cart>(api.delete(`/cart/items/${productId}`))
}

export function clearCart() {
  return api.delete('/cart')
}
