import { Link } from 'react-router'
import { FormError } from '../components/shared/Form'
import { useMyProblems } from '../features/problems/problemApi'
import { ProblemCard } from '../features/problems/ProblemCard'
import { errorMessage } from '../lib/api'

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
            <ProblemCard problem={p} />
          </li>
        ))}
      </ul>
    </div>
  )
}
