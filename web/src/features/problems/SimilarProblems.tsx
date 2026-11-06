import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useSimilarProblems } from './problemApi'

function useDebounced<T>(value: T, ms: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), ms)
    return () => clearTimeout(t)
  }, [value, ms])
  return debounced
}

/** "Already reported?" hints shown while someone writes a new problem (AI feature 2, simple version). */
export function SimilarProblems({ title, cityId, localityId }: { title: string; cityId: number | null; localityId: number | null }) {
  const debouncedTitle = useDebounced(title, 500)
  const similar = useSimilarProblems(debouncedTitle, cityId, localityId)

  if (!similar.data || similar.data.length === 0) return null
  return (
    <aside className="space-y-2 rounded-md border border-sky-200 bg-sky-50 p-3 text-sm" aria-label="Similar problems">
      <p className="font-medium">This may already have been reported. Would you like to support an existing one instead?</p>
      <ul className="list-disc space-y-1 pl-5">
        {similar.data.map((p) => (
          <li key={p.id}>
            <Link to={`/problems/${p.publicCode}`} target="_blank" className="text-udaan-700 underline">
              {p.title}
            </Link>{' '}
            <span className="text-stone-600">
              ({[p.localityName, p.cityName].filter(Boolean).join(', ')} · {p.supporters} supporters)
            </span>
          </li>
        ))}
      </ul>
      <p className="text-xs text-stone-600">If yours is different, carry on — you can still submit it.</p>
    </aside>
  )
}
