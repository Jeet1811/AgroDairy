import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, PackageX, Plus, TrendingDown } from 'lucide-react'
import * as inventoryApi from '@/api/inventory'
import * as productsApi from '@/api/products'
import { PageHeader } from '@/components/ui/PageHeader'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { Modal } from '@/components/ui/Modal'
import { BatchStatusBadge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatDate, todayIso } from '@/lib/format'
import type { BatchStatus, CreateInventoryBatchRequest } from '@/types/inventory'

const BATCH_STATUSES: BatchStatus[] = ['ACTIVE', 'EXPIRED', 'SOLD_OUT', 'DISCARDED']

function CreateBatchForm({ onClose }: { onClose: () => void }) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<CreateInventoryBatchRequest>({
    productId: '',
    batchCode: '',
    manufactureDate: todayIso(),
    quantityProduced: 1,
  })
  const [error, setError] = useState<string | null>(null)
  const { data: products } = useQuery({ queryKey: ['products', 'all-active'], queryFn: () => productsApi.listProducts({ active: true, size: 100 }) })

  const create = useMutation({
    mutationFn: () => inventoryApi.createBatch(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      toast.success('Batch created.')
      onClose()
    },
    onError: (err) => setError(apiErrorMessage(err, 'Could not create batch.')),
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (!form.productId) {
      setError('Please choose a product.')
      return
    }
    create.mutate()
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700">{error}</div>}
      <div>
        <label className="label" htmlFor="productId">
          Product
        </label>
        <select id="productId" className="input" value={form.productId} onChange={(e) => setForm((f) => ({ ...f, productId: e.target.value }))}>
          <option value="">Select a product…</option>
          {products?.items.map((product) => (
            <option key={product.id} value={product.id}>
              {product.name}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label className="label" htmlFor="batchCode">
          Batch code
        </label>
        <input
          id="batchCode"
          required
          className="input"
          value={form.batchCode}
          onChange={(e) => setForm((f) => ({ ...f, batchCode: e.target.value }))}
          placeholder="BATCH-2026-001"
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label" htmlFor="manufactureDate">
            Manufacture date
          </label>
          <input
            id="manufactureDate"
            type="date"
            className="input"
            value={form.manufactureDate}
            max={todayIso()}
            onChange={(e) => setForm((f) => ({ ...f, manufactureDate: e.target.value }))}
          />
        </div>
        <div>
          <label className="label" htmlFor="expiryDate">
            Expiry date <span className="font-normal text-brand-400">(optional)</span>
          </label>
          <input
            id="expiryDate"
            type="date"
            className="input"
            value={form.expiryDate ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, expiryDate: e.target.value || undefined }))}
          />
        </div>
      </div>
      <div>
        <label className="label" htmlFor="quantityProduced">
          Quantity produced
        </label>
        <input
          id="quantityProduced"
          type="number"
          min={1}
          className="input"
          value={form.quantityProduced}
          onChange={(e) => setForm((f) => ({ ...f, quantityProduced: Number(e.target.value) }))}
        />
      </div>
      <button type="submit" disabled={create.isPending} className="btn-primary w-full">
        {create.isPending ? 'Creating…' : 'Create batch'}
      </button>
    </form>
  )
}

export default function AdminInventoryPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<BatchStatus | ''>('')
  const [page, setPage] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)

  const { data: alerts } = useQuery({ queryKey: ['inventory', 'alerts'], queryFn: inventoryApi.getInventoryAlerts })
  const { data, isLoading } = useQuery({
    queryKey: ['inventory', 'batches', { status, page }],
    queryFn: () => inventoryApi.listBatches({ status: status || undefined, page, size: 15 }),
  })

  const updateStatus = useMutation({
    mutationFn: ({ id, batchStatus }: { id: string; batchStatus: BatchStatus }) => inventoryApi.updateBatch(id, { status: batchStatus }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
      toast.success('Batch status updated.')
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Could not update batch status.')),
  })

  return (
    <div>
      <PageHeader
        title="Inventory"
        description="Batch-level stock with FEFO allocation."
        action={
          <button onClick={() => setCreateOpen(true)} className="btn-primary">
            <Plus className="h-4 w-4" /> New batch
          </button>
        }
      />

      {alerts && (alerts.expiringSoon.length > 0 || alerts.lowStock.length > 0 || alerts.outOfStock.length > 0) && (
        <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-3">
          {alerts.expiringSoon.length > 0 && (
            <div className="card border-amber-200 bg-amber-50/60 p-4">
              <div className="flex items-center gap-2 font-semibold text-amber-800">
                <AlertTriangle className="h-4 w-4" /> Expiring soon ({alerts.expiringSoon.length})
              </div>
              <ul className="mt-3 space-y-1.5 text-sm text-amber-900">
                {alerts.expiringSoon.slice(0, 5).map((batch) => (
                  <li key={batch.batchId} className="flex justify-between">
                    <span className="truncate">{batch.productName}</span>
                    <span className="shrink-0 pl-2">{formatDate(batch.expiryDate)}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {alerts.lowStock.length > 0 && (
            <div className="card border-sky-200 bg-sky-50/60 p-4">
              <div className="flex items-center gap-2 font-semibold text-sky-800">
                <TrendingDown className="h-4 w-4" /> Low stock ({alerts.lowStock.length})
              </div>
              <ul className="mt-3 space-y-1.5 text-sm text-sky-900">
                {alerts.lowStock.slice(0, 5).map((item) => (
                  <li key={item.productId} className="flex justify-between">
                    <span className="truncate">{item.productName}</span>
                    <span className="shrink-0 pl-2">{item.totalAvailable} left</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {alerts.outOfStock.length > 0 && (
            <div className="card border-red-200 bg-red-50/60 p-4">
              <div className="flex items-center gap-2 font-semibold text-red-800">
                <PackageX className="h-4 w-4" /> Out of stock ({alerts.outOfStock.length})
              </div>
              <ul className="mt-3 space-y-1.5 text-sm text-red-900">
                {alerts.outOfStock.slice(0, 5).map((item) => (
                  <li key={item.productId} className="truncate">
                    {item.productName}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      <div className="mt-6">
        <select
          className="input w-auto"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as BatchStatus | '')
            setPage(0)
          }}
        >
          <option value="">All statuses</option>
          {BATCH_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      <div className="card mt-6 overflow-hidden">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState title="No batches found" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-brand-100 bg-brand-50/50 text-left text-xs uppercase tracking-wide text-brand-500">
                <tr>
                  <th className="px-4 py-3">Batch code</th>
                  <th className="hidden px-4 py-3 sm:table-cell">Expiry</th>
                  <th className="px-4 py-3">Available</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Update</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-50">
                {data.items.map((batch) => (
                  <tr key={batch.id} className="hover:bg-brand-50/40">
                    <td className="px-4 py-3 font-medium text-brand-900">{batch.batchCode}</td>
                    <td className="hidden px-4 py-3 text-brand-600 sm:table-cell">{batch.expiryDate ? formatDate(batch.expiryDate) : '—'}</td>
                    <td className="px-4 py-3 text-brand-700">{batch.quantityAvailable}</td>
                    <td className="px-4 py-3">
                      <BatchStatusBadge status={batch.status} />
                    </td>
                    <td className="px-4 py-3">
                      <select
                        className="input ml-auto w-auto !py-1.5 text-xs"
                        value={batch.status}
                        onChange={(e) => updateStatus.mutate({ id: batch.id, batchStatus: e.target.value as BatchStatus })}
                      >
                        {BATCH_STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {s}
                          </option>
                        ))}
                      </select>
                    </td>
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

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="New inventory batch">
        <CreateBatchForm onClose={() => setCreateOpen(false)} />
      </Modal>
    </div>
  )
}
