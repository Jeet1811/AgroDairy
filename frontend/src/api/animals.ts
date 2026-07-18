import { api, unwrap, unwrapPage } from '@/api/client'
import type {
  Animal,
  AnimalStatus,
  AnimalType,
  CreateAnimalRequest,
  CreateProductionRecordRequest,
  ProductionRecord,
  UpdateAnimalRequest,
} from '@/types/animal'

export interface ListAnimalsParams {
  status?: AnimalStatus
  type?: AnimalType
  page?: number
  size?: number
}

export function listAnimals(params: ListAnimalsParams = {}) {
  return unwrapPage<Animal>(api.get('/animals', { params }))
}

export function getAnimal(id: string) {
  return unwrap<Animal>(api.get(`/animals/${id}`))
}

export function createAnimal(request: CreateAnimalRequest) {
  return unwrap<Animal>(api.post('/animals', request))
}

export function updateAnimal(id: string, request: UpdateAnimalRequest) {
  return unwrap<Animal>(api.patch(`/animals/${id}`, request))
}

export function deleteAnimal(id: string) {
  return api.delete(`/animals/${id}`)
}

export function listProductionRecords(animalId: string, params: { from?: string; to?: string; page?: number; size?: number } = {}) {
  return unwrapPage<ProductionRecord>(api.get(`/animals/${animalId}/production`, { params }))
}

export function createProductionRecord(animalId: string, request: CreateProductionRecordRequest) {
  return unwrap<ProductionRecord>(api.post(`/animals/${animalId}/production`, request))
}
