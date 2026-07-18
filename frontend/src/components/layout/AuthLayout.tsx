import { Link } from 'react-router-dom'
import { Milk, Sparkles, TrendingUp, ShieldCheck } from 'lucide-react'
import type { ReactNode } from 'react'

const HIGHLIGHTS = [
  { icon: Sparkles, text: 'AI-powered demand forecasting for every product' },
  { icon: TrendingUp, text: 'Live dashboards across animals, orders & inventory' },
  { icon: ShieldCheck, text: 'FEFO-managed inventory so nothing goes to waste' },
]

export function AuthLayout({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <div className="grid min-h-screen grid-cols-1 lg:grid-cols-2">
      <div className="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-brand-700 via-brand-600 to-brand-800 p-10 text-white lg:flex">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-white/10 blur-3xl" />
        <div className="absolute -bottom-32 -left-16 h-80 w-80 rounded-full bg-brand-400/20 blur-3xl" />
        <Link to="/" className="relative flex items-center gap-2">
          <div className="rounded-lg bg-white/15 p-1.5 backdrop-blur">
            <Milk className="h-5 w-5 text-white" />
          </div>
          <span className="text-lg font-bold">AgroDairy AI</span>
        </Link>
        <div className="relative">
          <h1 className="text-3xl font-bold leading-tight">Run your dairy operation on real-time intelligence.</h1>
          <ul className="mt-8 space-y-4">
            {HIGHLIGHTS.map((item) => (
              <li key={item.text} className="flex items-center gap-3 text-brand-50">
                <span className="rounded-lg bg-white/15 p-2">
                  <item.icon className="h-4 w-4" />
                </span>
                <span className="text-sm">{item.text}</span>
              </li>
            ))}
          </ul>
        </div>
        <p className="relative text-xs text-brand-100">&copy; {new Date().getFullYear()} AgroDairy AI</p>
      </div>

      <div className="flex items-center justify-center bg-cream-50 px-4 py-12 sm:px-6">
        <div className="w-full max-w-sm">
          <Link to="/" className="mb-8 flex items-center gap-2 lg:hidden">
            <div className="rounded-lg bg-brand-600 p-1.5">
              <Milk className="h-5 w-5 text-white" />
            </div>
            <span className="text-lg font-bold text-brand-900">AgroDairy AI</span>
          </Link>
          <h2 className="text-2xl font-bold text-brand-900">{title}</h2>
          <p className="mt-1.5 text-sm text-brand-600">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>
    </div>
  )
}
