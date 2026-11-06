import { Link } from 'react-router'
import { apiUrl } from '../../lib/api'
import type { ProblemSummary } from './problemApi'
import { StatusBadge } from './StatusBadge'

const date = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeZone: 'Asia/Kolkata' })

export function ProblemCard({ problem: p }: { problem: ProblemSummary }) {
  return (
    <Link
      to={`/problems/${p.publicCode}`}
      className="flex gap-3 rounded-lg border border-stone-200 bg-white p-3 hover:border-udaan-600"
    >
      {p.thumbnailUrl ? (
        <img src={apiUrl(p.thumbnailUrl)} alt="" loading="lazy" className="h-20 w-20 flex-none rounded object-cover" />
      ) : (
        <div className="h-20 w-20 flex-none rounded bg-stone-100" aria-hidden="true" />
      )}
      <div className="min-w-0 flex-1 space-y-1">
        <p className="line-clamp-2 font-medium">{p.title}</p>
        <p className="text-xs text-stone-600">
          {p.categoryName} · {[p.localityName, p.cityName].filter(Boolean).join(', ')}
        </p>
        <p className="flex flex-wrap items-center gap-2 text-xs text-stone-500">
          <StatusBadge status={p.status} />
          <span>
            {p.supporters} {p.supporters === 1 ? 'supporter' : 'supporters'}
            {p.supportedByMe && ' (including you)'}
          </span>
          <span>
            {p.solutions} {p.solutions === 1 ? 'solution' : 'solutions'}
          </span>
          <span>{date.format(new Date(p.createdAt))}</span>
        </p>
      </div>
    </Link>
  )
}
