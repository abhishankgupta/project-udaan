import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useSession } from '../../lib/session'

/** Sends signed-out visitors to the sign-in page, then back here afterwards. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const user = useSession((s) => s.user)
  const restoring = useSession((s) => s.restoring)
  const location = useLocation()

  if (restoring) return <p className="text-stone-500">Signing you in…</p>
  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  return <>{children}</>
}
