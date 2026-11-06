import { Link } from 'react-router'
import { FormError } from '../components/shared/Form'
import { useMyProblems } from '../features/problems/problemApi'
import { StatusBadge } from '../features/problems/StatusBadge'
import { apiUrl, errorMessage } from '../lib/api'

const date = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeZone: 'Asia/Kolkata' })

export function MyProblemsPage() {
  const problems = useMyProblems()

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-bold">Your problems</h1>
        <Link to="/problems/new" className="rounded-md bg-udaan-600 px-3 py-2 text-sm font-semibold text-white hover:bg-udaan-700">
          + Raise a Problem
        </Link>
      </div>
      {problems.isPending && <p className="text-stone-500">Loading…</p>}
      {problems.isError && <FormError message={errorMessage(problems.error)} />}
      {problems.data?.length === 0 && <p className="text-stone-600">You haven't raised any problems yet.</p>}
      <ul className="space-y-3">
        {problems.data?.map((p) => (
          <li key={p.id}>
            <Link to={`/problems/${p.publicCode}`} className="flex gap-3 rounded-lg border border-stone-200 bg-white p-3 hover:border-udaan-600">
              {p.thumbnailUrl ? (
                <img src={apiUrl(p.thumbnailUrl)} alt="" className="h-16 w-16 flex-none rounded object-cover" />
              ) : (
                <div className="h-16 w-16 flex-none rounded bg-stone-100" aria-hidden="true" />
              )}
              <div className="min-w-0 space-y-1">
                <p className="truncate font-medium">{p.title}</p>
                <p className="flex flex-wrap items-center gap-2 text-xs text-stone-500">
                  <StatusBadge status={p.status} />
                  <span>{p.publicCode}</span>
                  <span>{[p.localityName, p.cityName].filter(Boolean).join(', ')}</span>
                  <span>{date.format(new Date(p.createdAt))}</span>
                </p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
