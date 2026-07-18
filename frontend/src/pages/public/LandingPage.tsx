import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowRight, BrainCircuit, Milk, PackageCheck, Repeat, ShieldCheck, Truck } from 'lucide-react'
import * as productsApi from '@/api/products'
import { ProductCard } from '@/components/products/ProductCard'
import { useAuth } from '@/context/AuthContext'

const FEATURES = [
  {
    icon: Milk,
    title: 'Farm-fresh, FEFO-managed',
    description: 'Every order is fulfilled from the earliest-expiring batch first, so quality never slips.',
  },
  {
    icon: Repeat,
    title: 'Flexible subscriptions',
    description: 'Daily, alternate-day, or custom weekday deliveries — pause, resume, or skip any time.',
  },
  {
    icon: BrainCircuit,
    title: 'AI demand forecasting',
    description: 'An XGBoost model predicts demand per product so we always stock the right amount.',
  },
  {
    icon: Truck,
    title: 'Reliable delivery',
    description: "Track every order from confirmation to your doorstep, with real-time status updates.",
  },
]

export default function LandingPage() {
  const { user } = useAuth()
  const { data } = useQuery({
    queryKey: ['products', 'featured'],
    queryFn: () => productsApi.listProducts({ size: 4, active: true }),
  })

  return (
    <div>
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-700 via-brand-600 to-brand-800 text-white">
        <div className="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-white/10 blur-3xl" />
        <div className="absolute -bottom-40 -left-20 h-96 w-96 rounded-full bg-accent-400/20 blur-3xl" />
        <div className="relative mx-auto max-w-7xl px-4 py-20 sm:px-6 sm:py-28 lg:px-8">
          <div className="max-w-2xl">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3.5 py-1.5 text-xs font-semibold uppercase tracking-wide backdrop-blur">
              <ShieldCheck className="h-3.5 w-3.5" /> Farm to doorstep, powered by AI
            </span>
            <h1 className="mt-6 text-4xl font-bold leading-tight sm:text-5xl">
              Fresh dairy, delivered on your schedule.
            </h1>
            <p className="mt-5 max-w-xl text-lg text-brand-50">
              AgroDairy AI brings together animal care, inventory, and demand forecasting so your subscription always
              arrives fresh — never late, never wasted.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/products" className="btn-accent !px-6 !py-3 text-base">
                Shop products <ArrowRight className="h-4 w-4" />
              </Link>
              {!user && (
                <Link to="/register" className="btn !bg-white/15 !px-6 !py-3 text-base text-white backdrop-blur hover:!bg-white/25">
                  Create an account
                </Link>
              )}
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((feature) => (
            <div key={feature.title} className="card p-6">
              <div className="w-fit rounded-xl bg-brand-50 p-3">
                <feature.icon className="h-5 w-5 text-brand-600" />
              </div>
              <h3 className="mt-4 font-semibold text-brand-900">{feature.title}</h3>
              <p className="mt-1.5 text-sm text-brand-600">{feature.description}</p>
            </div>
          ))}
        </div>
      </section>

      {data && data.items.length > 0 && (
        <section className="bg-white py-16">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="flex items-end justify-between">
              <div>
                <h2 className="text-2xl font-bold text-brand-900">Popular right now</h2>
                <p className="mt-1 text-brand-600">A taste of what's fresh in our catalogue.</p>
              </div>
              <Link to="/products" className="hidden items-center gap-1 text-sm font-semibold text-brand-700 hover:underline sm:flex">
                View all <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
            <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {data.items.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          </div>
        </section>
      )}

      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="card flex flex-col items-center gap-4 p-10 text-center sm:p-14">
          <div className="rounded-full bg-brand-50 p-3">
            <PackageCheck className="h-6 w-6 text-brand-600" />
          </div>
          <h2 className="text-2xl font-bold text-brand-900">Never run out of your daily essentials.</h2>
          <p className="max-w-lg text-brand-600">
            Set up a subscription once and let AgroDairy AI handle the rest — deliveries are generated automatically
            every evening for the next day.
          </p>
          <Link to={user ? '/subscriptions' : '/register'} className="btn-primary">
            {user ? 'Manage subscriptions' : 'Get started'} <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      </section>
    </div>
  )
}
