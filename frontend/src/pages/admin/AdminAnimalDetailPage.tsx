import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, Plus } from 'lucide-react'
import * as animalsApi from '@/api/animals'
import { PageSpinner, InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { AnimalStatusBadge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatDate, titleCase, todayIso } from '@/lib/format'
import type { AnimalStatus, Session } from '@/types/animal'

const ANIMAL_STATUSES: AnimalStatus[] = ['ACTIVE', 'MILKING', 'PREGNANT', 'DRY_PERIOD', 'UNDER_OBSERVATION', 'SOLD', 'DECEASED']

export default function AdminAnimalDetailPage() {
  const { id } = useParams<{ id: string }>()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [productionDate, setProductionDate] = useState(todayIso())
  const [session, setSession] = useState<Session>('MORNING')
  const [quantityLitres, setQuantityLitres] = useState('')
  const [recordError, setRecordError] = useState<string | null>(null)

  const { data: animal, isLoading } = useQuery({
    queryKey: ['animals', id],
    queryFn: () => animalsApi.getAnimal(id!),
    enabled: !!id,
  })

  const { data: records, isLoading: recordsLoading } = useQuery({
    queryKey: ['animals', id, 'production'],
    queryFn: () => animalsApi.listProductionRecords(id!, { size: 15 }),
    enabled: !!id,
  })

  const updateStatus = useMutation({
    mutationFn: (status: AnimalStatus) => animalsApi.updateAnimal(id!, { status }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['animals', id] })
      queryClient.invalidateQueries({ queryKey: ['animals'] })
      toast.success('Status updated.')
    },
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not update status.')),
  })

  const addRecord = useMutation({
    mutationFn: () =>
      animalsApi.createProductionRecord(id!, { productionDate, session, quantityLitres: Number(quantityLitres) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['animals', id, 'production'] })
      setQuantityLitres('')
      toast.success('Production recorded.')
    },
    onError: (err) => setRecordError(apiErrorMessage(err, 'Could not record production. Check for a duplicate entry.')),
  })

  function handleRecordSubmit(event: FormEvent) {
    event.preventDefault()
    setRecordError(null)
    if (!quantityLitres || Number(quantityLitres) < 0) {
      setRecordError('Enter a valid quantity.')
      return
    }
    addRecord.mutate()
  }

  if (isLoading) return <PageSpinner />
  if (!animal) return <EmptyState title="Animal not found" />

  return (
    <div>
      <Link to="/admin/animals" className="mb-6 flex items-center gap-1 text-sm font-medium text-brand-600 hover:text-brand-800">
        <ChevronLeft className="h-4 w-4" /> Back to animals
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-brand-900">{animal.tag}</h1>
          <p className="mt-1 text-brand-600">
            {titleCase(animal.type)}
            {animal.breed && ` · ${animal.breed}`}
          </p>
        </div>
        <AnimalStatusBadge status={animal.status} />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="card p-5 lg:col-span-1">
          <h2 className="font-semibold text-brand-900">Details</h2>
          <div className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-brand-500">Date of birth</span>
              <span className="font-medium text-brand-900">{animal.dateOfBirth ? formatDate(animal.dateOfBirth) : '—'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-brand-500">Lactation status</span>
              <span className="font-medium text-brand-900">{animal.lactationStatus ?? '—'}</span>
            </div>
            {animal.notes && (
              <div>
                <span className="text-brand-500">Notes</span>
                <p className="mt-1 text-brand-700">{animal.notes}</p>
              </div>
            )}
          </div>

          <div className="mt-5 border-t border-brand-100 pt-4">
            <label className="label" htmlFor="statusUpdate">
              Update status
            </label>
            <select
              id="statusUpdate"
              className="input"
              value={animal.status}
              disabled={updateStatus.isPending}
              onChange={(e) => updateStatus.mutate(e.target.value as AnimalStatus)}
            >
              {ANIMAL_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {titleCase(status)}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="lg:col-span-2">
          <form onSubmit={handleRecordSubmit} className="card space-y-4 p-5">
            <h2 className="font-semibold text-brand-900">Record production</h2>
            {recordError && <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700">{recordError}</div>}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div>
                <label className="label" htmlFor="productionDate">
                  Date
                </label>
                <input
                  id="productionDate"
                  type="date"
                  className="input"
                  value={productionDate}
                  max={todayIso()}
                  onChange={(e) => setProductionDate(e.target.value)}
                />
              </div>
              <div>
                <label className="label" htmlFor="session">
                  Session
                </label>
                <select id="session" className="input" value={session} onChange={(e) => setSession(e.target.value as Session)}>
                  <option value="MORNING">Morning</option>
                  <option value="EVENING">Evening</option>
                </select>
              </div>
              <div>
                <label className="label" htmlFor="quantity">
                  Quantity (L)
                </label>
                <input
                  id="quantity"
                  type="number"
                  step="0.1"
                  min="0"
                  className="input"
                  value={quantityLitres}
                  onChange={(e) => setQuantityLitres(e.target.value)}
                  placeholder="0.0"
                />
              </div>
            </div>
            <button type="submit" disabled={addRecord.isPending} className="btn-primary">
              <Plus className="h-4 w-4" /> {addRecord.isPending ? 'Saving…' : 'Add record'}
            </button>
          </form>

          <div className="card mt-6 overflow-hidden">
            <div className="border-b border-brand-100 px-5 py-3">
              <h2 className="font-semibold text-brand-900">Recent production</h2>
            </div>
            {recordsLoading ? (
              <InlineSpinner />
            ) : !records || records.items.length === 0 ? (
              <EmptyState title="No records yet" description="Recorded production will appear here." />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="border-b border-brand-100 bg-brand-50/50 text-left text-xs uppercase tracking-wide text-brand-500">
                    <tr>
                      <th className="px-5 py-2.5">Date</th>
                      <th className="px-5 py-2.5">Session</th>
                      <th className="px-5 py-2.5">Quantity</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-brand-50">
                    {records.items.map((record) => (
                      <tr key={record.id}>
                        <td className="px-5 py-2.5 text-brand-700">{formatDate(record.productionDate)}</td>
                        <td className="px-5 py-2.5 text-brand-700">{titleCase(record.session)}</td>
                        <td className="px-5 py-2.5 font-medium text-brand-900">{record.quantityLitres} L</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
