import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, Check } from 'lucide-react'
import * as aiApi from '@/api/ai'
import { PageHeader } from '@/components/ui/PageHeader'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { SeverityBadge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatDate, formatDateTime } from '@/lib/format'
import type { Severity } from '@/types/ai'

const SEVERITIES: Severity[] = ['LOW', 'MEDIUM', 'HIGH']

export default function AdminAnomaliesPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [acknowledged, setAcknowledged] = useState<'true' | 'false' | ''>('false')
  const [severity, setSeverity] = useState<Severity | ''>('')
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['ai', 'anomalies', { acknowledged, severity, page }],
    queryFn: () =>
      aiApi.listAnomalies({
        acknowledged: acknowledged === '' ? undefined : acknowledged === 'true',
        severity: severity || undefined,
        page,
        size: 15,
      }),
  })

  const acknowledge = useMutation({
    mutationFn: (id: string) => aiApi.acknowledgeAnomaly(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai', 'anomalies'] })
      toast.success('Anomaly acknowledged.')
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Could not acknowledge this anomaly.')),
  })

  return (
    <div>
      <PageHeader title="Anomalies" description="Operational production patterns flagged for review." />

      <div className="mt-6 flex flex-wrap gap-3">
        <select
          className="input w-auto"
          value={acknowledged}
          onChange={(e) => {
            setAcknowledged(e.target.value as 'true' | 'false' | '')
            setPage(0)
          }}
        >
          <option value="false">Unacknowledged</option>
          <option value="true">Acknowledged</option>
          <option value="">All</option>
        </select>
        <select
          className="input w-auto"
          value={severity}
          onChange={(e) => {
            setSeverity(e.target.value as Severity | '')
            setPage(0)
          }}
        >
          <option value="">All severities</option>
          {SEVERITIES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      <div className="mt-6">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={AlertTriangle} title="No anomalies found" description="Production drops beyond the statistical baseline will show up here." />
        ) : (
          <>
            <div className="space-y-3">
              {data.items.map((anomaly) => (
                <div key={anomaly.id} className="card flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-brand-900">{anomaly.animalTag}</p>
                      <SeverityBadge status={anomaly.severity} />
                    </div>
                    <p className="mt-1 text-sm text-brand-600">
                      Produced {anomaly.actualValue}L vs a {anomaly.baselineMean}L average (z-score {anomaly.zScore}) on{' '}
                      {formatDate(anomaly.productionDate)}
                    </p>
                    <p className="mt-1 text-xs text-brand-400">Detected {formatDateTime(anomaly.detectedAt)}</p>
                  </div>
                  {!anomaly.acknowledged && (
                    <button onClick={() => acknowledge.mutate(anomaly.id)} disabled={acknowledge.isPending} className="btn-secondary shrink-0">
                      <Check className="h-4 w-4" /> Acknowledge
                    </button>
                  )}
                </div>
              ))}
            </div>
            <div className="mt-4">
              <Pagination meta={data.meta} onPageChange={setPage} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
