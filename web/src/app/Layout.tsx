import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { authApi } from '../features/auth/authApi'
import { useRestoreSession } from '../features/auth/useRestoreSession'
import { config, isProduction } from '../lib/config'
import { useSession } from '../lib/session'

const navItems = [
  { to: '/', label: 'Home' },
  { to: '/problems', label: 'Explore Problems' },
  { to: '/solutions', label: 'Explore Solutions' },
  { to: '/organizations', label: 'Organizations' },
]

function AccountMenu() {
  const user = useSession((s) => s.user)
  const restoring = useSession((s) => s.restoring)
  const refreshToken = useSession((s) => s.refreshToken)
  const signOut = useSession((s) => s.signOut)
  const navigate = useNavigate()

  if (restoring) return null
  if (!user) {
    return (
      <Link to="/login" className="text-sm font-medium text-stone-700 hover:text-stone-900">
        Sign in
      </Link>
    )
  }
  return (
    <div className="flex items-center gap-3 text-sm">
      <Link to="/me/problems" className="text-stone-700 hover:text-stone-900">
        My problems
      </Link>
      <Link to="/me" className="font-medium text-stone-800 hover:text-udaan-700">
        {user.displayName}
      </Link>
      <button
        type="button"
        className="text-stone-500 hover:text-stone-900"
        onClick={() => {
          if (refreshToken) void authApi.logout(refreshToken).catch(() => undefined)
          signOut()
          navigate('/')
        }}
      >
        Sign out
      </button>
    </div>
  )
}

export function Layout() {
  useRestoreSession()

  return (
    <div className="min-h-screen bg-stone-50 text-stone-900">
      {!isProduction && (
        <div role="status" className="bg-amber-300 px-4 py-1 text-center text-sm font-medium">
          {config.appEnv.toUpperCase()} environment — data here is not real
        </div>
      )}
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-4 px-4 py-3">
          <Link to="/" className="text-xl font-bold text-udaan-700">
            Udaan
          </Link>
          <nav className="flex flex-1 flex-wrap gap-3 text-sm">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  isActive ? 'font-semibold text-udaan-700' : 'text-stone-600 hover:text-stone-900'
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <AccountMenu />
          <Link
            to="/problems/new"
            className="rounded-md bg-udaan-600 px-3 py-2 text-sm font-semibold text-white hover:bg-udaan-700"
          >
            + Raise a Problem
          </Link>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
      <footer className="mx-auto max-w-6xl px-4 py-8 text-xs text-stone-500">
        Udaan is a community platform, not an official government grievance system. In an emergency, call 112.
      </footer>
    </div>
  )
}
