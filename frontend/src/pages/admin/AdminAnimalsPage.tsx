import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import * as animalsApi from '@/api/animals'
import { PageHeader } from '@/components/ui/PageHeader'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { Modal } from '@/components/ui/Modal'
import { AnimalStatusBadge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatDate, titleCase } from '@/lib/format'
import type { AnimalStatus, AnimalType, CreateAnimalRequest } from '@/types/animal'

const ANIMAL_STATUSES: AnimalStatus[] = ['ACTIVE', 'MILKING', 'PREGNANT', 'DRY_PERIOD', 'UNDER_OBSERVATION', 'SOLD', 'DECEASED']
const ANIMAL_TYPES: AnimalType[] = ['COW', 'BUFFALO']

function CreateAnimalForm({ onClose }: { onClose: () => void }) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<CreateAnimalRequest>({ tag: '', type: 'COW', status: 'MILKING' })
  const [error, setError] = useState<string | null>(null)

  const create = useMutation({
    mutationFn: () => animalsApi.createAnimal(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['animals'] })
      toast.success('Animal added.')
      onClose()
    },
    onError: (err) => setError(apiErrorMessage(err, 'Could not add this animal.')),
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    create.mutate()
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700">{error}</div>}
      <div>
        <label className="label" htmlFor="tag">
          Tag
        </label>
        <input
          id="tag"
          required
          className="input"
          value={form.tag}
          onChange={(e) => setForm((f) => ({ ...f, tag: e.target.value }))}
          placeholder="COW-014"
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label" htmlFor="type">
            Type
          </label>
          <select id="type" className="input" value={form.type} onChange={(e) => setForm((f) => ({ ...f, type: e.target.value as AnimalType }))}>
            {ANIMAL_TYPES.map((type) => (
              <option key={type} value={type}>
                {titleCase(type)}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="status">
            Status
          </label>
          <select
            id="status"
            className="input"
            value={form.status}
            onChange={(e) => setForm((f) => ({ ...f, status: e.target.value as AnimalStatus }))}
          >
            {ANIMAL_STATUSES.map((status) => (
              <option key={status} value={status}>
                {titleCase(status)}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div>
        <label className="label" htmlFor="breed">
          Breed <span className="font-normal text-brand-400">(optional)</span>
        </label>
        <input id="breed" className="input" value={form.breed ?? ''} onChange={(e) => setForm((f) => ({ ...f, breed: e.target.value }))} />
      </div>
      <div>
        <label className="label" htmlFor="dateOfBirth">
          Date of birth <span className="font-normal text-brand-400">(optional)</span>
        </label>
        <input
          id="dateOfBirth"
          type="date"
          className="input"
          value={form.dateOfBirth ?? ''}
          onChange={(e) => setForm((f) => ({ ...f, dateOfBirth: e.target.value }))}
        />
      </div>
      <button type="submit" disabled={create.isPending} className="btn-primary w-full">
        {create.isPending ? 'Adding…' : 'Add animal'}
      </button>
    </form>
  )
}

export default function AdminAnimalsPage() {
  const [status, setStatus] = useState<AnimalStatus | ''>('')
  const [type, setType] = useState<AnimalType | ''>('')
  const [page, setPage] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['animals', { status, type, page }],
    queryFn: () => animalsApi.listAnimals({ status: status || undefined, type: type || undefined, page, size: 15 }),
  })

  return (
    <div>
      <PageHeader
        title="Animals"
        description="Track herd status and milk production."
        action={
          <button onClick={() => setCreateOpen(true)} className="btn-primary">
            <Plus className="h-4 w-4" /> Add animal
          </button>
        }
      />

      <div className="mt-6 flex flex-wrap gap-3">
        <select
          className="input w-auto"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as AnimalStatus | '')
            setPage(0)
          }}
        >
          <option value="">All statuses</option>
          {ANIMAL_STATUSES.map((s) => (
            <option key={s} value={s}>
              {titleCase(s)}
            </option>
          ))}
        </select>
        <select
          className="input w-auto"
          value={type}
          onChange={(e) => {
            setType(e.target.value as AnimalType | '')
            setPage(0)
          }}
        >
          <option value="">All types</option>
          {ANIMAL_TYPES.map((t) => (
            <option key={t} value={t}>
              {titleCase(t)}
            </option>
          ))}
        </select>
      </div>

      <div className="card mt-6 overflow-hidden">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState title="No animals found" description="Add your first animal to start tracking production." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-brand-100 bg-brand-50/50 text-left text-xs uppercase tracking-wide text-brand-500">
                <tr>
                  <th className="px-4 py-3">Tag</th>
                  <th className="hidden px-4 py-3 sm:table-cell">Type</th>
                  <th className="hidden px-4 py-3 md:table-cell">Breed</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="hidden px-4 py-3 lg:table-cell">Added</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-50">
                {data.items.map((animal) => (
                  <tr key={animal.id} className="hover:bg-brand-50/40">
                    <td className="px-4 py-3">
                      <Link to={`/admin/animals/${animal.id}`} className="font-semibold text-brand-800 hover:text-brand-600">
                        {animal.tag}
                      </Link>
                    </td>
                    <td className="hidden px-4 py-3 text-brand-600 sm:table-cell">{titleCase(animal.type)}</td>
                    <td className="hidden px-4 py-3 text-brand-600 md:table-cell">{animal.breed ?? '—'}</td>
                    <td className="px-4 py-3">
                      <AnimalStatusBadge status={animal.status} />
                    </td>
                    <td className="hidden px-4 py-3 text-brand-500 lg:table-cell">{formatDate(animal.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data && (
          <div className="px-4 pb-4">
            <Pagination meta={data.meta} onPageChange={setPage} />
          </div>
        )}
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Add animal">
        <CreateAnimalForm onClose={() => setCreateOpen(false)} />
      </Modal>
    </div>
  )
}
