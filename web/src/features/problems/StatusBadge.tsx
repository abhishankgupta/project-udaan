import { statusLabels, type ProblemStatus } from '../../lib/labels'

const tone: Record<ProblemStatus, string> = {
  SUBMITTED: 'bg-stone-200 text-stone-800',
  UNDER_REVIEW: 'bg-amber-100 text-amber-900',
  VERIFIED: 'bg-sky-100 text-sky-900',
  OPEN_FOR_SOLUTIONS: 'bg-sky-100 text-sky-900',
  SOLUTION_PROPOSED: 'bg-violet-100 text-violet-900',
  SOLUTION_ACCEPTED: 'bg-violet-100 text-violet-900',
  IN_PROGRESS: 'bg-orange-100 text-orange-900',
  RESOLVED: 'bg-green-100 text-green-900',
  CLOSED: 'bg-stone-200 text-stone-700',
}

export function StatusBadge({ status }: { status: ProblemStatus }) {
  return <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${tone[status]}`}>{statusLabels[status]}</span>
}
