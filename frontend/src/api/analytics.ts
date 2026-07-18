import { api, unwrap } from '@/api/client'
import type { Dashboard } from '@/types/analytics'

export function getDashboard() {
  return unwrap<Dashboard>(api.get('/analytics/dashboard'))
}
