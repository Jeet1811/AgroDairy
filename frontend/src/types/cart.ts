export interface CartItem {
  productId: string
  productName: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface Cart {
  items: CartItem[]
  total: number
}

export interface AddCartItemRequest {
  productId: string
  quantity: number
}

export interface UpdateCartItemRequest {
  quantity: number
}
