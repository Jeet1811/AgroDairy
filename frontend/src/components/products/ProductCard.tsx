import { Link } from 'react-router-dom'
import { Milk, Plus } from 'lucide-react'
import type { Product } from '@/types/product'
import { formatCurrency } from '@/lib/format'

interface ProductCardProps {
  product: Product
  onAddToCart?: (product: Product) => void
  adding?: boolean
}

export function ProductCard({ product, onAddToCart, adding }: ProductCardProps) {
  return (
    <div className="card group flex flex-col overflow-hidden transition hover:shadow-soft-lg">
      <Link to={`/products/${product.id}`} className="block aspect-[4/3] w-full overflow-hidden bg-brand-50">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <Milk className="h-10 w-10 text-brand-300" />
          </div>
        )}
      </Link>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <Link to={`/products/${product.id}`}>
          <h3 className="line-clamp-1 font-semibold text-brand-900 hover:text-brand-700">{product.name}</h3>
        </Link>
        {product.description && <p className="line-clamp-2 text-sm text-brand-500">{product.description}</p>}
        <div className="mt-auto flex items-center justify-between pt-2">
          <div>
            <p className="text-lg font-bold text-brand-900">{formatCurrency(product.price)}</p>
            <p className="text-xs text-brand-400">per {product.unit}</p>
          </div>
          {onAddToCart && (
            <button
              onClick={() => onAddToCart(product)}
              disabled={adding}
              className="btn-primary !px-3 !py-2"
              aria-label={`Add ${product.name} to cart`}
            >
              <Plus className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
