import { useState, type FormEvent } from 'react'
import { Button, FormError } from '../../components/shared/Form'
import { errorMessage } from '../../lib/api'
import { assistanceLabels, type AssistanceType } from '../../lib/labels'
import { useRequireSignIn } from '../auth/useRequireSignIn'
import type { SolutionView } from '../solutions/solutionApi'
import { useOfferHelp, useProblemContributions, useWithdrawOffer } from './contributionApi'

function OfferForm({ code, solutions, onDone }: { code: string; solutions: SolutionView[]; onDone: () => void }) {
  const [types, setTypes] = useState<AssistanceType[]>([])
  const [message, setMessage] = useState('')
  const [solutionId, setSolutionId] = useState('')
  const [shareContact, setShareContact] = useState(false)
  const [localError, setLocalError] = useState<string>()
  const offer = useOfferHelp(code)

  function submit(e: FormEvent) {
    e.preventDefault()
    if (types.length === 0) {
      setLocalError('Choose at least one way to help.')
      return
    }
    setLocalError(undefined)
    offer.mutate({ types, message, solutionId: solutionId || undefined, shareContact }, { onSuccess: onDone })
  }

  return (
    <form onSubmit={submit} className="space-y-3 rounded-lg border border-stone-200 bg-white p-4">
      <fieldset className="space-y-2">
        <legend className="text-sm font-medium">How can you help?</legend>
        <div className="grid gap-2 sm:grid-cols-2">
          {(Object.keys(assistanceLabels) as AssistanceType[]).map((t) => (
            <label key={t} className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={types.includes(t)}
                onChange={(e) => setTypes(e.target.checked ? [...types, t] : types.filter((x) => x !== t))}
              />
              {assistanceLabels[t]}
            </label>
          ))}
        </div>
      </fieldset>
      {solutions.length > 0 && (
        <label className="block space-y-1 text-sm">
          <span className="font-medium">Towards a specific solution (optional)</span>
          <select value={solutionId} onChange={(e) => setSolutionId(e.target.value)} className="w-full rounded-md border border-stone-300 bg-white px-3 py-2">
            <option value="">The problem in general</option>
            {solutions.map((s) => (
              <option key={s.id} value={s.id}>
                {s.title}
              </option>
            ))}
          </select>
        </label>
      )}
      <label className="block space-y-1 text-sm">
        <span className="font-medium">Message (optional)</span>
        <textarea
          rows={3}
          maxLength={1000}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="e.g. I'm a civil engineer and can visit on weekends."
          className="w-full rounded-md border border-stone-300 bg-white px-3 py-2"
        />
        <span className="block text-xs text-stone-500">Only the person who raised the problem (and the solution's proposer) can read this.</span>
      </label>
      <label className="flex items-start gap-2 text-sm">
        <input type="checkbox" className="mt-1" checked={shareContact} onChange={(e) => setShareContact(e.target.checked)} />
        <span>Share my email/phone with them so they can contact me.</span>
      </label>
      <FormError message={localError ?? (offer.isError ? errorMessage(offer.error) : undefined)} />
      <div className="flex gap-2">
        <Button type="submit" busy={offer.isPending}>
          Send offer
        </Button>
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </form>
  )
}

export function HelpOffers({ code, solutions }: { code: string; solutions: SolutionView[] }) {
  const offers = useProblemContributions(code)
  const withdraw = useWithdrawOffer()
  const requireSignIn = useRequireSignIn()
  const [open, setOpen] = useState(false)

  return (
    <section className="space-y-3" aria-labelledby="helpers-heading">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="helpers-heading" className="text-lg font-semibold">
          People offering help {offers.data && offers.data.length > 0 && `(${offers.data.length})`}
        </h2>
        {!open && (
          <Button variant="secondary" onClick={() => requireSignIn() && setOpen(true)}>
            I want to help
          </Button>
        )}
      </div>
      {open && <OfferForm code={code} solutions={solutions} onDone={() => setOpen(false)} />}
      {offers.data?.length === 0 && !open && <p className="text-sm text-stone-600">No offers yet. Be the first to help.</p>}
      <ul className="space-y-2">
        {offers.data?.map((o) => (
          <li key={o.id} className="rounded-md border border-stone-200 bg-white p-3 text-sm">
            <p>
              <span className="font-medium">{o.contributorName}</span> can help with{' '}
              {o.types.map((t) => assistanceLabels[t].toLowerCase()).join(', ')}
              {o.solutionTitle && <span className="text-stone-600"> · for “{o.solutionTitle}”</span>}
            </p>
            {o.message && <p className="mt-1 whitespace-pre-line text-stone-700">“{o.message}”</p>}
            {o.contact && !o.viewerIsContributor && (o.contact.email || o.contact.phone) && (
              <p className="mt-1 text-stone-700">
                Contact: {[o.contact.email, o.contact.phone].filter(Boolean).join(' · ')}
              </p>
            )}
            {o.viewerIsContributor && (
              <button type="button" className="mt-1 text-xs text-stone-500 underline" onClick={() => withdraw.mutate(o.id)}>
                Withdraw my offer
              </button>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
