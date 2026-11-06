import { useParams } from 'react-router'
import { FormError } from '../components/shared/Form'
import { Discussion } from '../features/discussion/Discussion'
import { SolutionCard } from '../features/solutions/SolutionCard'
import { useSolution } from '../features/solutions/solutionApi'
import { ApiError, errorMessage } from '../lib/api'
import { formatCost } from '../lib/format'

export function SolutionDetailPage() {
  const { id = '' } = useParams()
  const solution = useSolution(id)

  if (solution.isPending) return <p className="text-stone-500">Loading…</p>
  if (solution.isError) {
    return solution.error instanceof ApiError && solution.error.status === 404 ? (
      <div className="space-y-2">
        <h1 className="text-2xl font-bold">Solution not found</h1>
        <p className="text-stone-600">It may have been withdrawn.</p>
      </div>
    ) : (
      <FormError message={errorMessage(solution.error)} />
    )
  }

  const s = solution.data
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <SolutionCard solution={s} showProblem />
      <section className="space-y-3 rounded-lg border border-stone-200 bg-white p-4 text-sm">
        <div>
          <h2 className="font-semibold">How it would be done</h2>
          <p className="whitespace-pre-line">{s.approach}</p>
        </div>
        {s.resourcesRequired && (
          <div>
            <h2 className="font-semibold">Resources needed</h2>
            <p className="whitespace-pre-line">{s.resourcesRequired}</p>
          </div>
        )}
        {s.estimatedCostInr !== null && (
          <div>
            <h2 className="font-semibold">Estimated cost</h2>
            <p>{formatCost(s.estimatedCostInr)}</p>
          </div>
        )}
        {s.skills.length > 0 && (
          <div>
            <h2 className="font-semibold">Skills needed</h2>
            <ul className="mt-1 flex flex-wrap gap-2">
              {s.skills.map((k) => (
                <li key={k} className="rounded-full bg-stone-100 px-3 py-1">
                  {k}
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>
      <Discussion target={{ kind: 'solution', id: s.id }} />
    </div>
  )
}
