export type Severity = 'LOW' | 'MEDIUM' | 'HIGH'

export interface AnomalyAlert {
  id: string
  animalId: string
  animalTag: string
  detectedAt: string
  productionDate: string
  baselineMean: number
  baselineStddev: number
  actualValue: number
  zScore: number
  severity: Severity
  acknowledged: boolean
}

export interface DemandPrediction {
  date: string
  predictedQuantity: number
  confidenceLow: number
  confidenceHigh: number
}

export interface DemandForecastResponse {
  productId: string
  modelVersion: string
  predictions: DemandPrediction[]
}
