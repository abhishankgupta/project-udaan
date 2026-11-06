import { useParams } from 'react-router'
import { FormError } from '../components/shared/Form'
import { usePublicProfile } from '../features/profile/profileApi'
import { errorMessage } from '../lib/api'

const joined = new Intl.DateTimeFormat('en-IN', { month: 'long', year: 'numeric', timeZone: 'Asia/Kolkata' })

export function PublicProfilePage() {
  const { id = '' } = useParams()
  const profile = usePublicProfile(id)

  if (profile.isPending) return <p className="text-stone-500">Loading…</p>
  if (profile.isError) return <FormError message={errorMessage(profile.error)} />

  const p = profile.data
  const place = [p.locality, p.city].filter(Boolean).join(', ')
  return (
    <div className="mx-auto max-w-xl space-y-4">
      <div>
        <h1 className="text-2xl font-bold">{p.displayName}</h1>
        <p className="text-sm text-stone-500">
          {place && `${place} · `}Member since {joined.format(new Date(p.memberSince))}
        </p>
      </div>
      {p.bio && <p className="whitespace-pre-line">{p.bio}</p>}
      {p.skills.length > 0 && (
        <ul aria-label="Skills" className="flex flex-wrap gap-2">
          {p.skills.map((s) => (
            <li key={s} className="rounded-full bg-stone-200 px-3 py-1 text-sm">
              {s}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
