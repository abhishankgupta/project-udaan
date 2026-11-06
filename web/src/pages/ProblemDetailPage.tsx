import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { useRequireSignIn } from '../features/auth/useRequireSignIn'
import { FormError } from '../components/shared/Form'
import { HelpOffers } from '../features/contributions/HelpOffers'
import { Discussion } from '../features/discussion/Discussion'
import { ProblemActions } from '../features/problems/ProblemActions'
import { useProblem } from '../features/problems/problemApi'
import { StatusBadge } from '../features/problems/StatusBadge'
import { SolutionCard } from '../features/solutions/SolutionCard'
import { useProblemSolutions } from '../features/solutions/solutionApi'
import { ApiError, apiUrl, errorMessage } from '../lib/api'
import { assistanceLabels, statusLabels, urgencyLabels } from '../lib/labels'

const dateTime = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata' })

export function ProblemDetailPage() {
  const { code = '' } = useParams()
  const location = useLocation()
  const justSubmitted = (location.state as { justSubmitted?: boolean } | null)?.justSubmitted
  const problem = useProblem(code)

  if (problem.isPending) return <p className="text-stone-500">Loading…</p>
  if (problem.isError) {
    const notFound = problem.error instanceof ApiError && problem.error.status === 404
    return notFound ? (
      <div className="space-y-2">
        <h1 className="text-2xl font-bold">Problem not found</h1>
        <p className="text-stone-600">It may not exist, or it may still be waiting for review.</p>
      </div>
    ) : (
      <FormError message={errorMessage(problem.error)} />
    )
  }

  const p = problem.data
  const place = [p.place.localityName, p.place.cityName].filter(Boolean).join(', ')

  return (
    <article className="mx-auto max-w-3xl space-y-6">
      {justSubmitted && (
        <p role="status" className="rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-900">
          Thank you. Your problem was submitted as <strong>{p.publicCode}</strong>.{' '}
          {p.isPublic ? 'It is now public.' : "Moderators will review it shortly; you'll see it here in the meantime."}
        </p>
      )}
      {!justSubmitted && p.viewerIsOwner && !p.isPublic && (
        <p className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm">
          Only you can see this until moderators approve it.
        </p>
      )}

      <header className="space-y-2">
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <StatusBadge status={p.status} />
          <span className="text-stone-500">{p.publicCode}</span>
          <span className="text-stone-500">·</span>
          <span>{p.category.nameEn}</span>
        </div>
        <h1 className="text-2xl font-bold sm:text-3xl">{p.title}</h1>
        <p className="text-sm text-stone-600">
          {place}
          {p.place.addressText && ` · ${p.place.addressText}`} · Raised by{' '}
          {p.submittedBy.displayName ?? 'an anonymous resident'}
          {p.submittedBy.anonymous && p.viewerIsOwner && ' (shown as anonymous to others)'} on{' '}
          {dateTime.format(new Date(p.createdAt))}
        </p>
      </header>

      {p.isPublic && <ProblemActions problem={p} />}

      {p.photos.length > 0 && (
        <ul className="grid grid-cols-2 gap-2 sm:grid-cols-4" aria-label="Photos">
          {p.photos.map((photo, i) => (
            <li key={photo.id}>
              <a href={apiUrl(photo.url)} target="_blank" rel="noreferrer">
                <img
                  src={apiUrl(photo.url)}
                  alt={`Photo ${i + 1} of the problem`}
                  width={photo.width}
                  height={photo.height}
                  loading="lazy"
                  className="aspect-square w-full rounded-md object-cover"
                />
              </a>
            </li>
          ))}
        </ul>
      )}

      <section className="space-y-2">
        <h2 className="text-lg font-semibold">Description</h2>
        <p className="whitespace-pre-line">{p.description}</p>
      </section>

      <section className="grid gap-4 rounded-lg border border-stone-200 bg-white p-4 text-sm sm:grid-cols-2">
        <div>
          <h3 className="font-semibold">Desired outcome</h3>
          <p>{p.desiredOutcome}</p>
        </div>
        <div>
          <h3 className="font-semibold">Urgency</h3>
          <p>{urgencyLabels[p.urgency]}</p>
        </div>
        {p.peopleAffected && (
          <div>
            <h3 className="font-semibold">People affected</h3>
            <p>{p.peopleAffected.toLocaleString('en-IN')}</p>
          </div>
        )}
        {p.estimatedImpact && (
          <div>
            <h3 className="font-semibold">Impact</h3>
            <p>{p.estimatedImpact}</p>
          </div>
        )}
        <div className="sm:col-span-2">
          <h3 className="font-semibold">Help needed</h3>
          <ul className="mt-1 flex flex-wrap gap-2">
            {p.assistanceTypes.map((a) => (
              <li key={a} className="rounded-full bg-stone-100 px-3 py-1">
                {assistanceLabels[a]}
              </li>
            ))}
          </ul>
        </div>
        {p.place.latitude !== null && p.place.longitude !== null && (
          <div className="sm:col-span-2">
            <a
              className="text-udaan-700 underline"
              target="_blank"
              rel="noreferrer"
              href={`https://www.openstreetmap.org/?mlat=${p.place.latitude}&mlon=${p.place.longitude}#map=17/${p.place.latitude}/${p.place.longitude}`}
            >
              View the exact spot on a map
            </a>
          </div>
        )}
      </section>

      <section className="space-y-2">
        <h2 className="text-lg font-semibold">Timeline</h2>
        <ol className="space-y-2 border-l-2 border-stone-200 pl-4 text-sm">
          {p.timeline.map((t, i) => (
            <li key={i}>
              <p className="font-medium">{t.toStatus ? statusLabels[t.toStatus] : t.type}</p>
              {t.body && <p className="text-stone-600">{t.body}</p>}
              <p className="text-xs text-stone-500">{dateTime.format(new Date(t.at))}</p>
            </li>
          ))}
        </ol>
      </section>

      {p.isPublic && <ProblemCollaboration code={p.publicCode} acceptsSolutions={!['RESOLVED', 'CLOSED', 'IN_PROGRESS'].includes(p.status)} />}
    </article>
  )
}

/** Solutions, offers of help and discussion: the parts that only exist once a problem is public. */
function ProblemCollaboration({ code, acceptsSolutions }: { code: string; acceptsSolutions: boolean }) {
  const solutions = useProblemSolutions(code)
  const requireSignIn = useRequireSignIn()
  const navigate = useNavigate()

  return (
    <>
      <section className="space-y-3" aria-labelledby="solutions-heading">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 id="solutions-heading" className="text-lg font-semibold">
            Proposed solutions {solutions.data && solutions.data.length > 0 && `(${solutions.data.length})`}
          </h2>
          {acceptsSolutions && (
            <button
              type="button"
              className="rounded-md bg-udaan-600 px-3 py-2 text-sm font-semibold text-white hover:bg-udaan-700"
              onClick={() => requireSignIn() && navigate(`/problems/${code}/solutions/new`)}
            >
              Propose a solution
            </button>
          )}
        </div>
        {solutions.isError && <FormError message={errorMessage(solutions.error)} />}
        {solutions.data?.length === 0 && <p className="text-sm text-stone-600">No solutions yet. Have an idea? Propose it.</p>}
        <ul className="space-y-3">
          {solutions.data?.map((s) => (
            <li key={s.id}>
              <SolutionCard solution={s} />
            </li>
          ))}
        </ul>
      </section>
      <HelpOffers code={code} solutions={solutions.data?.filter((s) => s.status !== 'WITHDRAWN') ?? []} />
      <Discussion target={{ kind: 'problem', code }} />
      <p className="text-sm">
        <Link to="/me/contributions" className="text-udaan-700 underline">
          See all your contributions
        </Link>
      </p>
    </>
  )
}
