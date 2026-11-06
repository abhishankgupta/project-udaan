import { FormError } from '../components/shared/Form'
import { SolutionCard } from '../features/solutions/SolutionCard'
import { useRecentSolutions } from '../features/solutions/solutionApi'
import { errorMessage } from '../lib/api'

export function ExploreSolutionsPage() {
  const solutions = useRecentSolutions()
  return (
    <div className="space-y-4">
      <div className="space-y-1">
        <h1 className="text-2xl font-bold">Explore solutions</h1>
        <p className="text-stone-600">Recently proposed ideas for problems across the city. Endorse the ones you believe in.</p>
      </div>
      {solutions.isPending && <p className="text-stone-500">Loading…</p>}
      {solutions.isError && <FormError message={errorMessage(solutions.error)} />}
      {solutions.data?.length === 0 && <p className="text-stone-600">No solutions proposed yet.</p>}
      <ul className="grid gap-3 md:grid-cols-2">
        {solutions.data?.map((s) => (
          <li key={s.id}>
            <SolutionCard solution={s} showProblem />
          </li>
        ))}
      </ul>
    </div>
  )
}
