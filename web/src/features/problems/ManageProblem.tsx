import { useState, type FormEvent } from 'react'
import { Button, FormError, TextArea } from '../../components/shared/Form'
import { errorMessage } from '../../lib/api'
import type { ProblemStatus } from '../../lib/labels'
import { PhotoPicker } from './PhotoPicker'
import { usePostUpdate, useTransition, type Photo, type ProblemView } from './problemApi'

type Mode = null | 'update' | 'RESOLVED' | 'CLOSED'

/** For the problem's owner and the accepted solution's proposer: progress updates and status changes. */
export function ManageProblem({ problem: p }: { problem: ProblemView }) {
  const [mode, setMode] = useState<Mode>(null)
  const [text, setText] = useState('')
  const [photos, setPhotos] = useState<Photo[]>([])
  const [photosBusy, setPhotosBusy] = useState(false)
  const [localError, setLocalError] = useState<string>()
  const transition = useTransition(p.publicCode)
  const update = usePostUpdate(p.publicCode)

  const allowed = new Set<ProblemStatus>(p.permissions.allowedTransitions)
  if (!p.permissions.canPostUpdates && allowed.size === 0) return null

  function reset() {
    setMode(null)
    setText('')
    setPhotos([])
    setLocalError(undefined)
    transition.reset()
    update.reset()
  }

  function submit(e: FormEvent) {
    e.preventDefault()
    if (mode === 'update') {
      if (text.trim().length < 5) return setLocalError('Write a short update (at least 5 characters).')
      update.mutate({ body: text, mediaIds: photos.map((x) => x.id) }, { onSuccess: reset })
      return
    }
    if (text.trim().length < 10) {
      return setLocalError(mode === 'RESOLVED' ? 'Describe what was done (at least 10 characters).' : 'Explain why (at least 10 characters).')
    }
    if (mode === 'RESOLVED' && photos.length === 0) return setLocalError('Add at least one photo showing it is fixed.')
    transition.mutate({ toStatus: mode as ProblemStatus, note: text, evidenceMediaIds: photos.map((x) => x.id) }, { onSuccess: reset })
  }

  const error = localError ?? errorMessage(transition.error) ?? errorMessage(update.error)
  const busy = transition.isPending || update.isPending

  return (
    <section aria-labelledby="manage-heading" className="space-y-3 rounded-lg border border-udaan-600/40 bg-orange-50 p-4">
      <h2 id="manage-heading" className="font-semibold">
        {p.viewerIsOwner ? 'Manage your problem' : "You're delivering the accepted solution"}
      </h2>
      {mode === null ? (
        <div className="flex flex-wrap gap-2">
          {p.permissions.canPostUpdates && (
            <Button variant="secondary" onClick={() => setMode('update')}>
              Post a progress update
            </Button>
          )}
          {allowed.has('IN_PROGRESS') && p.status !== 'RESOLVED' && (
            <Button busy={busy} onClick={() => transition.mutate({ toStatus: 'IN_PROGRESS' })}>
              Mark work as started
            </Button>
          )}
          {allowed.has('RESOLVED') && <Button onClick={() => setMode('RESOLVED')}>Mark as resolved</Button>}
          {allowed.has('IN_PROGRESS') && p.status === 'RESOLVED' && (
            <Button variant="secondary" busy={busy} onClick={() => transition.mutate({ toStatus: 'IN_PROGRESS', note: 'Reopened by the owner' })}>
              Reopen
            </Button>
          )}
          {allowed.has('CLOSED') && (
            <Button variant="secondary" onClick={() => setMode('CLOSED')}>
              Close as not feasible
            </Button>
          )}
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-3">
          <TextArea
            label={mode === 'update' ? 'Progress update' : mode === 'RESOLVED' ? 'What was done to resolve it?' : 'Why is it being closed?'}
            maxLength={2000}
            rows={3}
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
          {mode !== 'CLOSED' && (
            <div className="space-y-1">
              <p className="text-sm font-medium">{mode === 'RESOLVED' ? 'Photos showing it is fixed (required)' : 'Photos (optional)'}</p>
              <PhotoPicker photos={photos} onChange={setPhotos} onBusyChange={setPhotosBusy} />
            </div>
          )}
          {mode === 'RESOLVED' && (
            <p className="text-xs text-stone-600">Followers and supporters will be asked to confirm. If enough people dispute it, it reopens.</p>
          )}
          <FormError message={error} />
          <div className="flex gap-2">
            <Button type="submit" busy={busy} disabled={photosBusy}>
              {mode === 'update' ? 'Post update' : mode === 'RESOLVED' ? 'Mark as resolved' : 'Close problem'}
            </Button>
            <Button type="button" variant="secondary" onClick={reset}>
              Cancel
            </Button>
          </div>
        </form>
      )}
      {mode === null && error && <p className="text-sm text-red-700">{error}</p>}
    </section>
  )
}
