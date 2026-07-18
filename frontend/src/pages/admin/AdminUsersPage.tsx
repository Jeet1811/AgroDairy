import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { UserPlus } from 'lucide-react'
import * as authApi from '@/api/auth'
import { PageHeader } from '@/components/ui/PageHeader'
import { useToast } from '@/context/ToastContext'
import { apiErrorMessage } from '@/api/client'
import type { CreateStaffRequest, Role } from '@/types/auth'

export default function AdminUsersPage() {
  const toast = useToast()
  const [form, setForm] = useState<CreateStaffRequest>({ email: '', password: '', fullName: '', role: 'STAFF' })
  const [error, setError] = useState<string | null>(null)

  const create = useMutation({
    mutationFn: () => authApi.createStaff(form),
    onSuccess: (response) => {
      toast.success(`${response.user.fullName} added as ${response.user.role}.`)
      setForm({ email: '', password: '', fullName: '', role: 'STAFF' })
    },
    onError: (err) => setError(apiErrorMessage(err, 'Could not create this account.')),
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    create.mutate()
  }

  return (
    <div>
      <PageHeader title="Users" description="Create staff and admin accounts." />

      <form onSubmit={handleSubmit} className="card mt-6 max-w-lg space-y-4 p-5">
        {error && <div className="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700">{error}</div>}
        <div>
          <label className="label" htmlFor="fullName">
            Full name
          </label>
          <input id="fullName" required className="input" value={form.fullName} onChange={(e) => setForm((f) => ({ ...f, fullName: e.target.value }))} />
        </div>
        <div>
          <label className="label" htmlFor="email">
            Email
          </label>
          <input
            id="email"
            type="email"
            required
            className="input"
            value={form.email}
            onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
          />
        </div>
        <div>
          <label className="label" htmlFor="phone">
            Phone <span className="font-normal text-brand-400">(optional)</span>
          </label>
          <input id="phone" className="input" value={form.phone ?? ''} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />
        </div>
        <div>
          <label className="label" htmlFor="password">
            Temporary password
          </label>
          <input
            id="password"
            type="password"
            required
            minLength={8}
            className="input"
            value={form.password}
            onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
            placeholder="At least 8 characters"
          />
        </div>
        <div>
          <label className="label">Role</label>
          <div className="flex gap-2">
            {(['STAFF', 'ADMIN'] as Role[]).map((role) => (
              <button
                key={role}
                type="button"
                onClick={() => setForm((f) => ({ ...f, role }))}
                className={`flex-1 rounded-xl border px-3 py-2 text-sm font-medium transition ${
                  form.role === role ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-200 text-brand-700 hover:bg-brand-50'
                }`}
              >
                {role}
              </button>
            ))}
          </div>
        </div>
        <button type="submit" disabled={create.isPending} className="btn-primary w-full">
          <UserPlus className="h-4 w-4" /> {create.isPending ? 'Creating…' : 'Create account'}
        </button>
      </form>
    </div>
  )
}
