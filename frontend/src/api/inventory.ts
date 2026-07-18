import { api, unwrap, unwrapPage } from '@/api/client'
import type {
  BatchStatus,
  CreateInventoryBatchRequest,
  InventoryAlerts,
  InventoryBatch,
  UpdateInventoryBatchRequest,
} from '@/types/inventory'

export interface ListBatchesParams {
  productId?: string
  status?: BatchStatus
  expiringWithinDays?: number
  page?: number
  size?: number
}

export function listBatches(params: ListBatchesParams = {}) {
  return unwrapPage<InventoryBatch>(api.get('/inventory/batches', { params }))
}

export function createBatch(request: CreateInventoryBatchRequest) {
  return unwrap<InventoryBatch>(api.post('/inventory/batches', request))
}

export function updateBatch(id: string, request: UpdateInventoryBatchRequest) {
  return unwrap<InventoryBatch>(api.patch(`/inventory/batches/${id}`, request))
}

export function getInventoryAlerts() {
  return unwrap<InventoryAlerts>(api.get('/inventory/alerts'))
}
