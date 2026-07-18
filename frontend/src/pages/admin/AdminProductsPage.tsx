import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Tag as TagIcon, Trash2 } from 'lucide-react'
import * as productsApi from '@/api/products'
import { PageHeader } from '@/components/ui/PageHeader'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { Modal } from '@/components/ui/Modal'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Badge } from '@/components/ui/Badge'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatCurrency } from '@/lib/format'
import type { CategoryKind, CreateCategoryRequest, CreateProductRequest, Product } from '@/types/product'

const CATEGORY_KINDS: CategoryKind[] = ['DAIRY', 'AGRICULTURE']

function CreateCategoryForm({ onClose }: { onClose: () => void }) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<CreateCategoryRequest>({ name: '', kind: 'DAIRY' })
  const [error, setError] = useState<string | null>(null)

  const create = useMutation({
    mutationFn: () => productsApi.createCategory(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      toast.success('Category created.')
      onClose()
    },
    onError: (err) => setError(apiErrorMessage(err, 'Could not create category.')),
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
        <label className="label" htmlFor="categoryName">
          Name
        </label>
        <input id="categoryName" required className="input" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
      </div>
      <div>
        <label className="label" htmlFor="categoryKind">
          Kind
        </label>
        <select id="categoryKind" className="input" value={form.kind} onChange={(e) => setForm((f) => ({ ...f, kind: e.target.value as CategoryKind }))}>
          {CATEGORY_KINDS.map((kind) => (
            <option key={kind} value={kind}>
              {kind}
            </option>
          ))}
        </select>
      </div>
      <button type="submit" disabled={create.isPending} className="btn-primary w-full">
        {create.isPending ? 'Creating…' : 'Create category'}
      </button>
    </form>
  )
}

function ProductForm({ initial, onSubmit, submitting, submitLabel }: {
  initial: CreateProductRequest
  onSubmit: (values: CreateProductRequest) => void
  submitting: boolean
  submitLabel: string
}) {
  const [form, setForm] = useState<CreateProductRequest>(initial)
  const [error, setError] = useState<string | null>(null)
  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: productsApi.listCategories })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (!form.categoryId) {
      setError('Please choose a category.')
      return
    }
    onSubmit(form)
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700">{error}</div>}
      <div>
        <label className="label" htmlFor="name">
          Name
        </label>
        <input id="name" required className="input" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
      </div>
      <div>
        <label className="label" htmlFor="categoryId">
          Category
        </label>
        <select id="categoryId" className="input" value={form.categoryId} onChange={(e) => setForm((f) => ({ ...f, categoryId: e.target.value }))}>
          <option value="">Select category…</option>
          {categories?.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label" htmlFor="price">
            Price (₹)
          </label>
          <input
            id="price"
            type="number"
            step="0.01"
            min="0"
            required
            className="input"
            value={form.price}
            onChange={(e) => setForm((f) => ({ ...f, price: Number(e.target.value) }))}
          />
        </div>
        <div>
          <label className="label" htmlFor="unit">
            Unit
          </label>
          <input id="unit" required className="input" value={form.unit} onChange={(e) => setForm((f) => ({ ...f, unit: e.target.value }))} placeholder="bottle, kg, pack" />
        </div>
      </div>
      <div>
        <label className="label" htmlFor="shelfLifeDays">
          Shelf life (days) <span className="font-normal text-brand-400">(optional)</span>
        </label>
        <input
          id="shelfLifeDays"
          type="number"
          min="1"
          className="input"
          value={form.shelfLifeDays ?? ''}
          onChange={(e) => setForm((f) => ({ ...f, shelfLifeDays: e.target.value ? Number(e.target.value) : undefined }))}
        />
      </div>
      <div>
        <label className="label" htmlFor="description">
          Description <span className="font-normal text-brand-400">(optional)</span>
        </label>
        <textarea id="description" rows={2} className="input" value={form.description ?? ''} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
      </div>
      <button type="submit" disabled={submitting} className="btn-primary w-full">
        {submitting ? 'Saving…' : submitLabel}
      </button>
    </form>
  )
}

export default function AdminProductsPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [categoryId, setCategoryId] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [categoryModalOpen, setCategoryModalOpen] = useState(false)
  const [editing, setEditing] = useState<Product | null>(null)
  const [deactivating, setDeactivating] = useState<Product | null>(null)

  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: productsApi.listCategories })
  const { data, isLoading } = useQuery({
    queryKey: ['products', 'admin', { categoryId, page }],
    queryFn: () => productsApi.listProducts({ categoryId: categoryId || undefined, page, size: 15 }),
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['products'] })
  }

  const create = useMutation({
    mutationFn: (values: CreateProductRequest) => productsApi.createProduct(values),
    onSuccess: () => {
      invalidate()
      toast.success('Product created.')
      setCreateOpen(false)
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Could not create product.')),
  })

  const update = useMutation({
    mutationFn: (values: CreateProductRequest) => productsApi.updateProduct(editing!.id, values),
    onSuccess: () => {
      invalidate()
      toast.success('Product updated.')
      setEditing(null)
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Could not update product.')),
  })

  const deactivate = useMutation({
    mutationFn: () => productsApi.deactivateProduct(deactivating!.id),
    onSuccess: () => {
      invalidate()
      toast.success('Product deactivated.')
      setDeactivating(null)
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Could not deactivate product.')),
  })

  return (
    <div>
      <PageHeader
        title="Products"
        description="Manage your catalogue and pricing."
        action={
          <div className="flex gap-2">
            <button onClick={() => setCategoryModalOpen(true)} className="btn-secondary">
              <TagIcon className="h-4 w-4" /> New category
            </button>
            <button onClick={() => setCreateOpen(true)} className="btn-primary">
              <Plus className="h-4 w-4" /> New product
            </button>
          </div>
        }
      />

      <div className="mt-6">
        <select
          className="input w-auto"
          value={categoryId}
          onChange={(e) => {
            setCategoryId(e.target.value)
            setPage(0)
          }}
        >
          <option value="">All categories</option>
          {categories?.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </div>

      <div className="card mt-6 overflow-hidden">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState title="No products found" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-brand-100 bg-brand-50/50 text-left text-xs uppercase tracking-wide text-brand-500">
                <tr>
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Price</th>
                  <th className="hidden px-4 py-3 sm:table-cell">Unit</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-50">
                {data.items.map((product) => (
                  <tr key={product.id} className="hover:bg-brand-50/40">
                    <td className="px-4 py-3 font-medium text-brand-900">{product.name}</td>
                    <td className="px-4 py-3 text-brand-700">{formatCurrency(product.price)}</td>
                    <td className="hidden px-4 py-3 text-brand-600 sm:table-cell">{product.unit}</td>
                    <td className="px-4 py-3">
                      <Badge tone={product.active ? 'green' : 'gray'}>{product.active ? 'Active' : 'Inactive'}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        <button onClick={() => setEditing(product)} className="btn-ghost !px-2.5 !py-1.5 text-xs">
                          Edit
                        </button>
                        {product.active && (
                          <button onClick={() => setDeactivating(product)} className="rounded-lg p-1.5 text-red-500 hover:bg-red-50" aria-label="Deactivate">
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
                      </div>
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

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="New product">
        <ProductForm
          initial={{ categoryId: '', name: '', price: 0, unit: '' }}
          onSubmit={(values) => create.mutate(values)}
          submitting={create.isPending}
          submitLabel="Create product"
        />
      </Modal>

      <Modal open={!!editing} onClose={() => setEditing(null)} title="Edit product">
        {editing && (
          <ProductForm
            initial={{
              categoryId: editing.categoryId,
              name: editing.name,
              description: editing.description ?? undefined,
              price: editing.price,
              unit: editing.unit,
              shelfLifeDays: editing.shelfLifeDays ?? undefined,
              imageUrl: editing.imageUrl ?? undefined,
            }}
            onSubmit={(values) => update.mutate(values)}
            submitting={update.isPending}
            submitLabel="Save changes"
          />
        )}
      </Modal>

      <Modal open={categoryModalOpen} onClose={() => setCategoryModalOpen(false)} title="New category">
        <CreateCategoryForm onClose={() => setCategoryModalOpen(false)} />
      </Modal>

      <ConfirmDialog
        open={!!deactivating}
        title="Deactivate product"
        message={`"${deactivating?.name}" will no longer be available for purchase. Continue?`}
        confirmLabel="Deactivate"
        loading={deactivate.isPending}
        onConfirm={() => deactivate.mutate()}
        onClose={() => setDeactivating(null)}
      />
    </div>
  )
}
