import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PackageCheck } from 'lucide-react'
import * as cartApi from '@/api/cart'
import * as ordersApi from '@/api/orders'
import { InlineSpinner } from '@/components/ui/Spinner'
import { apiErrorMessage } from '@/api/client'
import { formatCurrency } from '@/lib/format'

export default function CheckoutPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [deliveryAddress, setDeliveryAddress] = useState('')
  const [error, setError] = useState<string | null>(null)

  const { data: cart, isLoading } = useQuery({ queryKey: ['cart'], queryFn: cartApi.getCart })

  const placeOrder = useMutation({
    mutationFn: () => ordersApi.createOrder({ deliveryAddress }),
    onSuccess: (order) => {
      // Navigate before invalidating the cart query — otherwise this page's
      // `cart.items.length === 0` guard can redirect to /cart first, racing
      // this navigate to the new order.
      navigate(`/orders/${order.id}`, { replace: true })
      queryClient.invalidateQueries({ queryKey: ['cart'] })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: (err) => setError(apiErrorMessage(err, 'Could not place your order. Please try again.')),
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    placeOrder.mutate()
  }

  if (isLoading) return <InlineSpinner />
  // Once the order succeeds, invalidating the cart query can resolve to an empty cart
  // while this page is still mounted, mid-navigation to /orders/:id — render nothing
  // rather than let the empty-cart guard below race that navigate with its own redirect.
  if (placeOrder.isSuccess) return null
  if (!cart || cart.items.length === 0) return <Navigate to="/cart" replace />

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold text-brand-900">Checkout</h1>

      <div className="mt-8 grid grid-cols-1 gap-8 lg:grid-cols-3">
        <form onSubmit={handleSubmit} className="card space-y-4 p-5 lg:col-span-2">
          {error && <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700">{error}</div>}
          <div>
            <label className="label" htmlFor="deliveryAddress">
              Delivery address
            </label>
            <textarea
              id="deliveryAddress"
              required
              rows={4}
              className="input"
              value={deliveryAddress}
              onChange={(e) => setDeliveryAddress(e.target.value)}
              placeholder="House no., street, locality, city, PIN code"
            />
          </div>
          <button type="submit" disabled={placeOrder.isPending} className="btn-primary w-full">
            <PackageCheck className="h-4 w-4" />
            {placeOrder.isPending ? 'Placing order…' : `Place order — ${formatCurrency(cart.total)}`}
          </button>
        </form>

        <div className="card h-fit p-5">
          <h2 className="font-semibold text-brand-900">Order items</h2>
          <div className="mt-4 space-y-3">
            {cart.items.map((item) => (
              <div key={item.productId} className="flex justify-between text-sm">
                <span className="text-brand-700">
                  {item.productName} <span className="text-brand-400">&times;{item.quantity}</span>
                </span>
                <span className="font-medium text-brand-900">{formatCurrency(item.subtotal)}</span>
              </div>
            ))}
          </div>
          <div className="mt-4 flex justify-between border-t border-brand-100 pt-3 font-bold text-brand-900">
            <span>Total</span>
            <span>{formatCurrency(cart.total)}</span>
          </div>
        </div>
      </div>
    </div>
  )
}
