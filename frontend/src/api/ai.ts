import { api, unwrap, unwrapPage } from '@/api/client'
import type { AnomalyAlert, DemandForecastResponse, Severity } from '@/types/ai'

export interface ListAnomaliesParams {
  acknowledged?: boolean
  severity?: Severity
  page?: number
  size?: number
}

export function listAnomalies(params: ListAnomaliesParams = {}) {
  return unwrapPage<AnomalyAlert>(api.get('/ai/anomalies', { params }))
}

export function acknowledgeAnomaly(id: string) {
  return unwrap<AnomalyAlert>(api.patch(`/ai/anomalies/${id}/ack`))
}

export function getDemandForecast(productId: string, days = 7) {
  return unwrap<DemandForecastResponse>(api.get('/ai/demand-forecast', { params: { productId, days } }))
}
