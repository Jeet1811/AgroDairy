import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Area, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { BrainCircuit } from 'lucide-react'
import * as aiApi from '@/api/ai'
import * as productsApi from '@/api/products'
import { PageHeader } from '@/components/ui/PageHeader'
import { InlineSpinner } from '@/components/ui/Spinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { formatDate } from '@/lib/format'

export default function AdminForecastPage() {
  const [productId, setProductId] = useState('')
  const [days, setDays] = useState(7)

  const { data: products } = useQuery({ queryKey: ['products', 'all-active'], queryFn: () => productsApi.listProducts({ active: true, size: 100 }) })

  const { data: forecast, isLoading, isError, error } = useQuery({
    queryKey: ['ai', 'forecast', productId, days],
    queryFn: () => aiApi.getDemandForecast(productId, days),
    enabled: !!productId,
  })

  const chartData = forecast?.predictions.map((p) => ({
    label: formatDate(p.date),
    range: [p.confidenceLow, p.confidenceHigh],
    predictedQuantity: p.predictedQuantity,
  }))

  return (
    <div>
      <PageHeader title="Demand forecast" description="AI-predicted demand per product for the days ahead." />

      <div className="mt-6 flex flex-wrap items-end gap-3">
        <div className="w-64">
          <label className="label" htmlFor="product">
            Product
          </label>
          <select id="product" className="input" value={productId} onChange={(e) => setProductId(e.target.value)}>
            <option value="">Select a product…</option>
            {products?.items.map((product) => (
              <option key={product.id} value={product.id}>
                {product.name}
              </option>
            ))}
          </select>
        </div>
        <div className="w-32">
          <label className="label" htmlFor="days">
            Horizon (days)
          </label>
          <input id="days" type="number" min={1} max={30} className="input" value={days} onChange={(e) => setDays(Number(e.target.value))} />
        </div>
      </div>

      <div className="card mt-6 p-5">
        {!productId ? (
          <EmptyState icon={BrainCircuit} title="Choose a product" description="Select a product above to see its demand forecast." />
        ) : isLoading ? (
          <InlineSpinner />
        ) : isError ? (
          <EmptyState
            title="No forecast available"
            description={
              (error as { response?: { data?: { error?: { message?: string } } } })?.response?.data?.error?.message ??
              'This product may not have a trained model yet.'
            }
          />
        ) : (
          <>
            <p className="text-xs font-medium uppercase tracking-wide text-brand-500">Model: {forecast?.modelVersion}</p>
            <div className="mt-4 h-72">
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={chartData} margin={{ left: -10 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e1f1d8" />
                  <XAxis dataKey="label" tick={{ fontSize: 11, fill: '#529c39' }} />
                  <YAxis tick={{ fontSize: 11, fill: '#529c39' }} width={50} />
                  <Tooltip />
                  <Area dataKey="range" stroke="none" fill="#9ccf82" fillOpacity={0.35} name="Confidence range" />
                  <Line type="monotone" dataKey="predictedQuantity" stroke="#3f7e2c" strokeWidth={2.5} dot={{ r: 3 }} name="Predicted quantity" />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
