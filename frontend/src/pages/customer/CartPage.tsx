import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Minus, Plus, ShoppingBag, Trash2 } from 'lucide-react'
import * as cartApi from '@/api/cart'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import { formatCurrency } from '@/lib/format'

export default function CartPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()

  const { data: cart, isLoading } = useQuery({ queryKey: ['cart'], queryFn: cartApi.getCart })

  const updateQuantity = useMutation({
    mutationFn: ({ productId, quantity }: { productId: string; quantity: number }) => cartApi.updateCartItem(productId, { quantity }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cart'] }),
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not update quantity.')),
  })

  const removeItem = useMutation({
    mutationFn: (productId: string) => cartApi.removeCartItem(productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cart'] }),
    onError: (error) => toast.error(apiErrorMessage(error, 'Could not remove item.')),
  })

  if (isLoading) return <InlineSpinner />

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold text-brand-900">Your cart</h1>

      {!cart || cart.items.length === 0 ? (
        <div className="mt-8">
          <EmptyState
            icon={ShoppingBag}
            title="Your cart is empty"
            description="Browse the shop to add fresh dairy and farm produce."
            action={
              <Link to="/products" className="btn-primary">
                Start shopping
              </Link>
            }
          />
        </div>
      ) : (
        <div className="mt-8 grid grid-cols-1 gap-8 lg:grid-cols-3">
          <div className="space-y-4 lg:col-span-2">
            {cart.items.map((item) => (
              <div key={item.productId} className="card flex items-center gap-4 p-4">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold text-brand-900">{item.productName}</p>
                  <p className="text-sm text-brand-500">{formatCurrency(item.unitPrice)} each</p>
                </div>
                <div className="flex items-center rounded-xl border border-brand-200">
                  <button
                    onClick={() => updateQuantity.mutate({ productId: item.productId, quantity: Math.max(1, item.quantity - 1) })}
                    className="p-2 text-brand-600 hover:bg-brand-50"
                    aria-label="Decrease quantity"
                  >
                    <Minus className="h-3.5 w-3.5" />
                  </button>
                  <span className="w-8 text-center text-sm font-semibold text-brand-900">{item.quantity}</span>
                  <button
                    onClick={() => updateQuantity.mutate({ productId: item.productId, quantity: item.quantity + 1 })}
                    className="p-2 text-brand-600 hover:bg-brand-50"
                    aria-label="Increase quantity"
                  >
                    <Plus className="h-3.5 w-3.5" />
                  </button>
                </div>
                <p className="w-24 text-right font-semibold text-brand-900">{formatCurrency(item.subtotal)}</p>
                <button
                  onClick={() => removeItem.mutate(item.productId)}
                  className="rounded-lg p-2 text-red-500 hover:bg-red-50"
                  aria-label="Remove item"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>

          <div className="card h-fit p-5">
            <h2 className="font-semibold text-brand-900">Order summary</h2>
            <div className="mt-4 flex justify-between text-sm text-brand-600">
              <span>Subtotal</span>
              <span>{formatCurrency(cart.total)}</span>
            </div>
            <div className="mt-2 flex justify-between border-t border-brand-100 pt-3 font-bold text-brand-900">
              <span>Total</span>
              <span>{formatCurrency(cart.total)}</span>
            </div>
            <button onClick={() => navigate('/checkout')} className="btn-primary mt-5 w-full">
              Proceed to checkout
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
