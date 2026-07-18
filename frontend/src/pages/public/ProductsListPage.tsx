import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import * as productsApi from '@/api/products'
import * as cartApi from '@/api/cart'
import { ProductCard } from '@/components/products/ProductCard'
import { EmptyState } from '@/components/ui/EmptyState'
import { InlineSpinner } from '@/components/ui/Spinner'
import { Pagination } from '@/components/ui/Pagination'
import { useAuth } from '@/context/AuthContext'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import type { Product } from '@/types/product'
import { cn } from '@/lib/cn'

const PAGE_SIZE = 12

export default function ProductsListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState<string | undefined>(undefined)
  const [page, setPage] = useState(0)

  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: productsApi.listCategories })

  const { data, isLoading } = useQuery({
    queryKey: ['products', { search, categoryId, page }],
    queryFn: () => productsApi.listProducts({ search: search || undefined, categoryId, active: true, page, size: PAGE_SIZE }),
  })

  const addToCart = useMutation({
    mutationFn: (product: Product) => cartApi.addCartItem({ productId: product.id, quantity: 1 }),
    onSuccess: (_, product) => {
      queryClient.invalidateQueries({ queryKey: ['cart'] })
      toast.success(`${product.name} added to cart.`)
    },
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not add item to cart.')),
  })

  function handleAddToCart(product: Product) {
    if (!user) {
      navigate('/login')
      return
    }
    if (user.role !== 'CUSTOMER') return
    addToCart.mutate(product)
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-brand-900">Shop our catalogue</h1>
          <p className="mt-1 text-brand-600">Fresh dairy and farm produce, ready to order or subscribe to.</p>
        </div>
        <div className="relative w-full sm:w-72">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-brand-400" />
          <input
            className="input pl-9"
            placeholder="Search products…"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(0)
            }}
          />
        </div>
      </div>

      {categories && categories.length > 0 && (
        <div className="mt-6 flex flex-wrap gap-2">
          <button
            onClick={() => {
              setCategoryId(undefined)
              setPage(0)
            }}
            className={cn(
              'rounded-full border px-3.5 py-1.5 text-sm font-medium transition',
              !categoryId ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-200 text-brand-700 hover:bg-brand-50',
            )}
          >
            All
          </button>
          {categories.map((category) => (
            <button
              key={category.id}
              onClick={() => {
                setCategoryId(category.id)
                setPage(0)
              }}
              className={cn(
                'rounded-full border px-3.5 py-1.5 text-sm font-medium transition',
                categoryId === category.id
                  ? 'border-brand-600 bg-brand-600 text-white'
                  : 'border-brand-200 text-brand-700 hover:bg-brand-50',
              )}
            >
              {category.name}
            </button>
          ))}
        </div>
      )}

      <div className="mt-8">
        {isLoading ? (
          <InlineSpinner />
        ) : !data || data.items.length === 0 ? (
          <EmptyState title="No products found" description="Try a different search term or category." />
        ) : (
          <>
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {data.items.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  onAddToCart={!user || user.role === 'CUSTOMER' ? handleAddToCart : undefined}
                  adding={addToCart.isPending && addToCart.variables?.id === product.id}
                />
              ))}
            </div>
            <div className="mt-8">
              <Pagination meta={data.meta} onPageChange={setPage} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
