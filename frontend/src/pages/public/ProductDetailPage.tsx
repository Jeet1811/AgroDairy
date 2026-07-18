import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, Milk, Minus, Plus, ShoppingCart, Star } from 'lucide-react'
import * as productsApi from '@/api/products'
import * as cartApi from '@/api/cart'
import * as reviewsApi from '@/api/reviews'
import { PageSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { useAuth } from '@/context/AuthContext'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatCurrency, formatDate } from '@/lib/format'
import { cn } from '@/lib/cn'

function StarDisplay({ rating }: { rating: number }) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map((n) => (
        <Star key={n} className={cn('h-4 w-4', n <= rating ? 'fill-accent-400 text-accent-400' : 'text-brand-200')} />
      ))}
    </div>
  )
}

function StarInput({ value, onChange }: { value: number; onChange: (value: number) => void }) {
  return (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map((n) => (
        <button key={n} type="button" onClick={() => onChange(n)} aria-label={`${n} star`}>
          <Star className={cn('h-6 w-6 transition', n <= value ? 'fill-accent-400 text-accent-400' : 'text-brand-200 hover:text-accent-300')} />
        </button>
      ))}
    </div>
  )
}

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [quantity, setQuantity] = useState(1)
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [reviewError, setReviewError] = useState<string | null>(null)

  const { data: product, isLoading } = useQuery({
    queryKey: ['products', id],
    queryFn: () => productsApi.getProduct(id!),
    enabled: !!id,
  })

  const { data: reviews } = useQuery({
    queryKey: ['reviews', id],
    queryFn: () => reviewsApi.listReviews(id!, { size: 20 }),
    enabled: !!id,
  })

  const addToCart = useMutation({
    mutationFn: () => cartApi.addCartItem({ productId: id!, quantity }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] })
      toast.success('Added to cart.')
    },
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not add item to cart.')),
  })

  const submitReview = useMutation({
    mutationFn: () => reviewsApi.createReview(id!, { rating, comment: comment || undefined }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews', id] })
      setComment('')
      setRating(5)
      toast.success('Thanks for your review!')
    },
    onError: (error) => setReviewError(apiErrorMessage(error, 'Could not submit your review.')),
  })

  function handleAddToCart() {
    if (!user) {
      navigate('/login')
      return
    }
    addToCart.mutate()
  }

  function handleReviewSubmit(event: FormEvent) {
    event.preventDefault()
    setReviewError(null)
    submitReview.mutate()
  }

  if (isLoading) return <PageSpinner />
  if (!product) return <EmptyState title="Product not found" description="This product may have been removed." />

  const averageRating = reviews && reviews.items.length > 0 ? reviews.items.reduce((sum, r) => sum + r.rating, 0) / reviews.items.length : null

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <Link to="/products" className="mb-6 flex items-center gap-1 text-sm font-medium text-brand-600 hover:text-brand-800">
        <ChevronLeft className="h-4 w-4" /> Back to shop
      </Link>

      <div className="grid grid-cols-1 gap-10 lg:grid-cols-2">
        <div className="aspect-square overflow-hidden rounded-2xl bg-brand-50">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full w-full items-center justify-center">
              <Milk className="h-16 w-16 text-brand-300" />
            </div>
          )}
        </div>

        <div>
          <h1 className="text-3xl font-bold text-brand-900">{product.name}</h1>
          {averageRating !== null && (
            <div className="mt-2 flex items-center gap-2">
              <StarDisplay rating={Math.round(averageRating)} />
              <span className="text-sm text-brand-500">
                {averageRating.toFixed(1)} ({reviews!.items.length} review{reviews!.items.length === 1 ? '' : 's'})
              </span>
            </div>
          )}
          <p className="mt-4 text-3xl font-bold text-brand-900">{formatCurrency(product.price)}</p>
          <p className="text-sm text-brand-500">per {product.unit}</p>
          {product.description && <p className="mt-4 text-brand-600">{product.description}</p>}
          {product.shelfLifeDays && (
            <p className="mt-2 text-sm text-brand-500">Shelf life: {product.shelfLifeDays} day(s) from manufacture</p>
          )}

          {(!user || user.role === 'CUSTOMER') && (
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <div className="flex items-center rounded-xl border border-brand-200">
                <button onClick={() => setQuantity((q) => Math.max(1, q - 1))} className="p-3 text-brand-600 hover:bg-brand-50" aria-label="Decrease quantity">
                  <Minus className="h-4 w-4" />
                </button>
                <span className="w-10 text-center font-semibold text-brand-900">{quantity}</span>
                <button onClick={() => setQuantity((q) => q + 1)} className="p-3 text-brand-600 hover:bg-brand-50" aria-label="Increase quantity">
                  <Plus className="h-4 w-4" />
                </button>
              </div>
              <button onClick={handleAddToCart} disabled={addToCart.isPending} className="btn-primary flex-1 sm:flex-none">
                <ShoppingCart className="h-4 w-4" />
                {addToCart.isPending ? 'Adding…' : 'Add to cart'}
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="mt-16 border-t border-brand-100 pt-10">
        <h2 className="text-xl font-bold text-brand-900">Reviews</h2>

        {user?.role === 'CUSTOMER' && (
          <form onSubmit={handleReviewSubmit} className="card mt-5 max-w-lg space-y-3 p-5">
            {reviewError && <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700">{reviewError}</div>}
            <div>
              <label className="label">Your rating</label>
              <StarInput value={rating} onChange={setRating} />
            </div>
            <div>
              <label className="label" htmlFor="comment">
                Comment <span className="font-normal text-brand-400">(optional)</span>
              </label>
              <textarea
                id="comment"
                className="input"
                rows={3}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Share your experience with this product…"
              />
            </div>
            <button type="submit" disabled={submitReview.isPending} className="btn-secondary">
              {submitReview.isPending ? 'Submitting…' : 'Submit review'}
            </button>
          </form>
        )}

        <div className="mt-6 space-y-4">
          {!reviews || reviews.items.length === 0 ? (
            <p className="text-sm text-brand-500">No reviews yet — be the first to share your thoughts.</p>
          ) : (
            reviews.items.map((review) => (
              <div key={review.id} className="card p-4">
                <div className="flex items-center justify-between">
                  <StarDisplay rating={review.rating} />
                  <span className="text-xs text-brand-400">{formatDate(review.createdAt)}</span>
                </div>
                {review.comment && <p className="mt-2 text-sm text-brand-700">{review.comment}</p>}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}
