import { Button } from '../../components/shared/Form'
import { errorMessage } from '../../lib/api'
import { useRequireSignIn } from '../auth/useRequireSignIn'
import { useResolutionVote, type ProblemView } from './problemApi'

const date = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeZone: 'Asia/Kolkata' })

/** "Is it really fixed?" — community confirmation while a resolution is fresh. */
export function ResolutionCheck({ problem: p }: { problem: ProblemView }) {
  const vote = useResolutionVote(p.publicCode)
  const requireSignIn = useRequireSignIn()
  const r = p.resolution
  if (!r) return null

  return (
    <section aria-labelledby="resolution-heading" className="space-y-2 rounded-lg border border-green-300 bg-green-50 p-4 text-sm">
      <h2 id="resolution-heading" className="font-semibold">
        Marked as resolved
      </h2>
      <p>
        {r.confirms} {r.confirms === 1 ? 'person confirms' : 'people confirm'} it's fixed · {r.disputes}{' '}
        {r.disputes === 1 ? 'says' : 'say'} it isn't
      </p>
      {r.votingOpen && !p.viewerIsOwner && (
        <>
          <p>Have you seen it? Let others know.</p>
          <div className="flex flex-wrap gap-2">
            <Button
              variant={r.viewerVote === 'CONFIRM' ? 'primary' : 'secondary'}
              aria-pressed={r.viewerVote === 'CONFIRM'}
              busy={vote.isPending && vote.variables === 'CONFIRM'}
              onClick={() => requireSignIn() && vote.mutate('CONFIRM')}
            >
              Yes, it's fixed
            </Button>
            <Button
              variant={r.viewerVote === 'DISPUTE' ? 'primary' : 'secondary'}
              aria-pressed={r.viewerVote === 'DISPUTE'}
              busy={vote.isPending && vote.variables === 'DISPUTE'}
              onClick={() => requireSignIn() && vote.mutate('DISPUTE')}
            >
              No, it's not fixed
            </Button>
          </div>
        </>
      )}
      {r.votingOpen && r.votingClosesAt && (
        <p className="text-xs text-stone-600">
          Open until {date.format(new Date(r.votingClosesAt))}. If at least 3 people say it isn't fixed (and they outnumber confirmations), it
          reopens.
        </p>
      )}
      {vote.isError && <p className="text-red-700">{errorMessage(vote.error)}</p>}
    </section>
  )
}
