import { GoogleLogin, type CredentialResponse } from '@react-oauth/google'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { apiErrorMessage } from '@/api/client'
import { GOOGLE_CLIENT_ID } from '@/lib/googleAuth'
import type { User } from '@/types/auth'

function landingPathFor(user: User): string {
  return user.role === 'CUSTOMER' ? '/products' : '/admin'
}

export function GoogleSignInButton({ onError }: { onError: (message: string) => void }) {
  const { loginWithGoogle } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // Not configured on this deployment (no VITE_GOOGLE_CLIENT_ID) — omit the button entirely
  // rather than rendering one that can never work.
  if (!GOOGLE_CLIENT_ID) return null

  async function handleSuccess(credentialResponse: CredentialResponse) {
    if (!credentialResponse.credential) {
      onError('Google did not return a sign-in token. Please try again.')
      return
    }
    try {
      const user = await loginWithGoogle(credentialResponse.credential)
      const from = (location.state as { from?: Location })?.from?.pathname
      navigate(from && from !== '/login' ? from : landingPathFor(user), { replace: true })
    } catch (err) {
      onError(apiErrorMessage(err, 'Could not sign in with Google.'))
    }
  }

  return (
    <div>
      <div className="my-5 flex items-center gap-3 text-xs font-medium uppercase tracking-wide text-brand-400">
        <div className="h-px flex-1 bg-brand-100" />
        or
        <div className="h-px flex-1 bg-brand-100" />
      </div>
      <div className="flex justify-center">
        <GoogleLogin
          onSuccess={handleSuccess}
          onError={() => onError('Could not sign in with Google.')}
          width={320}
        />
      </div>
    </div>
  )
}
