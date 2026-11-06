import { Link } from 'react-router'
import { useMyContributions, useWithdrawOffer } from '../features/contributions/contributionApi'
import { ProblemCard } from '../features/problems/ProblemCard'
import { useMyProblems } from '../features/problems/problemApi'
import { SolutionCard } from '../features/solutions/SolutionCard'
import { useMySolutions } from '../features/solutions/solutionApi'
import { assistanceLabels } from '../lib/labels'

export function MyContributionsPage() {
  const problems = useMyProblems()
  const solutions = useMySolutions()
  const offers = useMyContributions()
  const withdraw = useWithdrawOffer()

  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <h1 className="text-2xl font-bold">My contributions</h1>

      <section className="space-y-3" aria-labelledby="mine-problems">
        <div className="flex items-center justify-between gap-2">
          <h2 id="mine-problems" className="text-lg font-semibold">
            Problems you raised
          </h2>
          <Link to="/problems/new" className="text-sm text-udaan-700 underline">
            + Raise a problem
          </Link>
        </div>
        {problems.data?.length === 0 && <p className="text-sm text-stone-600">None yet.</p>}
        <ul className="space-y-2">
          {problems.data?.map((p) => (
            <li key={p.id}>
              <ProblemCard problem={p} />
            </li>
          ))}
        </ul>
      </section>

      <section className="space-y-3" aria-labelledby="mine-solutions">
        <h2 id="mine-solutions" className="text-lg font-semibold">
          Solutions you proposed
        </h2>
        {solutions.data?.length === 0 && <p className="text-sm text-stone-600">None yet.</p>}
        <ul className="space-y-2">
          {solutions.data?.map((s) => (
            <li key={s.id}>
              {s.status === 'WITHDRAWN' ? (
                <p className="rounded-md border border-dashed border-stone-300 p-3 text-sm text-stone-500">
                  {s.title} — withdrawn
                </p>
              ) : (
                <SolutionCard solution={s} showProblem />
              )}
            </li>
          ))}
        </ul>
      </section>

      <section className="space-y-3" aria-labelledby="mine-offers">
        <h2 id="mine-offers" className="text-lg font-semibold">
          Your offers of help
        </h2>
        {offers.data?.length === 0 && <p className="text-sm text-stone-600">None yet.</p>}
        <ul className="space-y-2">
          {offers.data?.map((o) => (
            <li key={o.id} className="rounded-md border border-stone-200 bg-white p-3 text-sm">
              <p>
                <Link to={`/problems/${o.problemCode}`} className="font-medium underline">
                  {o.problemTitle}
                </Link>
                {o.solutionTitle && <span className="text-stone-600"> · for “{o.solutionTitle}”</span>}
              </p>
              <p className="text-stone-600">{o.types.map((t) => assistanceLabels[t]).join(', ')}</p>
              <button type="button" className="mt-1 text-xs text-stone-500 underline" onClick={() => withdraw.mutate(o.id)}>
                Withdraw
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
