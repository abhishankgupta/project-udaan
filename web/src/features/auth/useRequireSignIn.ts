import { useLocation, useNavigate } from 'react-router'
import { useSession } from '../../lib/session'

/** Returns a guard: true when signed in, otherwise sends the visitor to sign in and back. */
export function useRequireSignIn() {
  const signedIn = useSession((s) => s.user !== null)
  const navigate = useNavigate()
  const location = useLocation()
  return () => {
    if (signedIn) return true
    navigate(`/login?next=${encodeURIComponent(location.pathname)}`)
    return false
  }
}
