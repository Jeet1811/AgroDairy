export type BatchStatus = 'ACTIVE' | 'EXPIRED' | 'SOLD_OUT' | 'DISCARDED'

export interface InventoryBatch {
  id: string
  productId: string
  batchCode: string
  manufactureDate: string
  expiryDate: string | null
  quantityProduced: number
  quantityAvailable: number
  quantitySold: number
  status: BatchStatus
  createdAt: string
  updatedAt: string
}

export interface CreateInventoryBatchRequest {
  productId: string
  batchCode: string
  manufactureDate: string
  expiryDate?: string
  quantityProduced: number
}

export interface UpdateInventoryBatchRequest {
  status?: BatchStatus
}

export interface ExpiringSoonBatch {
  batchId: string
  productId: string
  productName: string
  batchCode: string
  expiryDate: string
  quantityAvailable: number
}

export interface LowStockProduct {
  productId: string
  productName: string
  totalAvailable: number
}

export interface OutOfStockProduct {
  productId: string
  productName: string
}

export interface InventoryAlerts {
  expiringSoon: ExpiringSoonBatch[]
  lowStock: LowStockProduct[]
  outOfStock: OutOfStockProduct[]
}
