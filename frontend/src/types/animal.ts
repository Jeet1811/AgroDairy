export type AnimalStatus = 'ACTIVE' | 'MILKING' | 'PREGNANT' | 'DRY_PERIOD' | 'UNDER_OBSERVATION' | 'SOLD' | 'DECEASED'
export type AnimalType = 'COW' | 'BUFFALO'
export type Session = 'MORNING' | 'EVENING'

export interface Animal {
  id: string
  tag: string
  type: AnimalType
  breed: string | null
  dateOfBirth: string | null
  status: AnimalStatus
  lactationStatus: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface ProductionRecord {
  id: string
  animalId: string
  productionDate: string
  session: Session
  quantityLitres: number
  recordedBy: string | null
  createdAt: string
}

export interface CreateAnimalRequest {
  tag: string
  type: AnimalType
  breed?: string
  dateOfBirth?: string
  status: AnimalStatus
  notes?: string
}

export type UpdateAnimalRequest = Partial<CreateAnimalRequest>

export interface CreateProductionRecordRequest {
  productionDate: string
  session: Session
  quantityLitres: number
}
