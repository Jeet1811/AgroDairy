import type { ReactNode } from 'react'
import { GoogleOAuthProvider } from '@react-oauth/google'
import { GOOGLE_CLIENT_ID } from '@/lib/googleAuth'

// Only wrap with GoogleOAuthProvider when a client ID is actually configured — GoogleSignInButton
// checks the same env var and renders nothing otherwise, so the two stay consistent.
export function GoogleAuthGate({ children }: { children: ReactNode }) {
  return GOOGLE_CLIENT_ID ? <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>{children}</GoogleOAuthProvider> : children
}
