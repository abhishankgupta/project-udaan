import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { Button } from '../../components/shared/Form'
import { errorMessage } from '../../lib/api'
import { useSession } from '../../lib/session'
import { useToggle, type ProblemView } from './problemApi'

/** Support, follow and share for a public problem. Signed-out visitors are sent to sign in first. */
export function ProblemActions({ problem: p }: { problem: ProblemView }) {
  const signedIn = useSession((s) => s.user !== null)
  const navigate = useNavigate()
  const location = useLocation()
  const support = useToggle(p.publicCode, 'support')
  const follow = useToggle(p.publicCode, 'follow')
  const [shareNote, setShareNote] = useState<string>()

  function requireSignIn(): boolean {
    if (signedIn) return true
    navigate(`/login?next=${encodeURIComponent(location.pathname)}`)
    return false
  }

  async function share() {
    const url = window.location.href
    const text = `${p.title} — support this problem on Udaan`
    if (typeof navigator.share === 'function') {
      try {
        await navigator.share({ title: p.title, text, url })
        return
      } catch {
        // The user cancelled, or sharing failed; fall through to copying.
      }
    }
    try {
      await navigator.clipboard.writeText(url)
      setShareNote('Link copied.')
    } catch {
      setShareNote(url)
    }
  }

  const error = support.error ?? follow.error

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-2">
        {!p.viewerIsOwner && (
          <Button
            variant={p.viewerSupports ? 'secondary' : 'primary'}
            busy={support.isPending}
            aria-pressed={p.viewerSupports}
            onClick={() => requireSignIn() && support.mutate(!p.viewerSupports)}
          >
            {p.viewerSupports ? '✓ Supported' : 'Support this problem'} · {p.counts.supporters}
          </Button>
        )}
        {p.viewerIsOwner && (
          <span className="text-sm text-stone-600">
            {p.counts.supporters} {p.counts.supporters === 1 ? 'person supports' : 'people support'} this problem
          </span>
        )}
        <Button
          variant="secondary"
          busy={follow.isPending}
          aria-pressed={p.viewerFollows}
          onClick={() => requireSignIn() && follow.mutate(!p.viewerFollows)}
        >
          {p.viewerFollows ? '✓ Following' : 'Follow'}
        </Button>
        <Button variant="secondary" onClick={() => void share()}>
          Share
        </Button>
        <a
          className="text-sm text-udaan-700 underline"
          target="_blank"
          rel="noreferrer"
          href={`https://wa.me/?text=${encodeURIComponent(`${p.title} — ${typeof window === 'undefined' ? '' : window.location.href}`)}`}
        >
          Share on WhatsApp
        </a>
      </div>
      {shareNote && <p className="text-sm text-stone-600">{shareNote}</p>}
      {error && <p className="text-sm text-red-700">{errorMessage(error)}</p>}
    </div>
  )
}
