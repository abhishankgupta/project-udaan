import { Link } from 'react-router'
import { Button } from '../../components/shared/Form'
import { errorMessage } from '../../lib/api'
import { formatCost } from '../../lib/format'
import { useRequireSignIn } from '../auth/useRequireSignIn'
import { useEndorse, useSolutionAction, type SolutionView } from './solutionApi'

/** One solution with endorse / accept / withdraw controls appropriate to the viewer. */
export function SolutionCard({ solution: s, showProblem }: { solution: SolutionView; showProblem?: boolean }) {
  const endorse = useEndorse(s)
  const accept = useSolutionAction(s, 'accept')
  const withdraw = useSolutionAction(s, 'withdraw')
  const requireSignIn = useRequireSignIn()
  const error = endorse.error ?? accept.error ?? withdraw.error

  return (
    <article className={`space-y-2 rounded-lg border bg-white p-4 ${s.status === 'ACCEPTED' ? 'border-green-400' : 'border-stone-200'}`}>
      {s.status === 'ACCEPTED' && <p className="text-xs font-semibold uppercase tracking-wide text-green-800">Accepted solution</p>}
      {showProblem && (
        <p className="text-xs text-stone-500">
          For:{' '}
          <Link to={`/problems/${s.problem.publicCode}`} className="underline">
            {s.problem.title}
          </Link>
        </p>
      )}
      <h3 className="font-semibold">
        <Link to={`/solutions/${s.id}`} className="hover:underline">
          {s.title}
        </Link>
      </h3>
      <p className="text-sm text-stone-700">{s.summary}</p>
      <p className="flex flex-wrap gap-x-3 text-xs text-stone-500">
        <span>By {s.proposer.displayName}</span>
        {s.estimatedCostInr !== null && <span>Est. cost {formatCost(s.estimatedCostInr)}</span>}
        {s.timelineText && <span>Timeline: {s.timelineText}</span>}
        <span>
          {s.endorsements} {s.endorsements === 1 ? 'endorsement' : 'endorsements'}
        </span>
      </p>
      <div className="flex flex-wrap gap-2">
        {!s.viewerIsProposer && (
          <Button
            variant="secondary"
            busy={endorse.isPending}
            aria-pressed={s.viewerEndorsed}
            onClick={() => requireSignIn() && endorse.mutate(!s.viewerEndorsed)}
          >
            {s.viewerEndorsed ? '✓ Endorsed' : 'Endorse'}
          </Button>
        )}
        {s.canAccept && (
          <Button
            busy={accept.isPending}
            onClick={() => {
              if (window.confirm(`Accept “${s.title}” as the solution to your problem?`)) accept.mutate()
            }}
          >
            Accept this solution
          </Button>
        )}
        {s.viewerIsProposer && s.status === 'PUBLISHED' && (
          <Button
            variant="secondary"
            busy={withdraw.isPending}
            onClick={() => {
              if (window.confirm('Withdraw your solution? It will no longer be shown.')) withdraw.mutate()
            }}
          >
            Withdraw
          </Button>
        )}
        <Link to={`/solutions/${s.id}`} className="self-center text-sm text-udaan-700 underline">
          Details and discussion
        </Link>
      </div>
      {error && <p className="text-sm text-red-700">{errorMessage(error)}</p>}
    </article>
  )
}
