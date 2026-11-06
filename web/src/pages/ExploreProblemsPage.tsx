import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { Button, FormError } from '../components/shared/Form'
import { useCities, useLocalities } from '../features/geo/geoApi'
import { ProblemCard } from '../features/problems/ProblemCard'
import { useFeed, type FeedFilters, type FeedSort } from '../features/problems/problemApi'
import { useCategories } from '../features/taxonomy/useCategories'
import { errorMessage } from '../lib/api'
import { assistanceLabels, statusLabels, urgencyLabels } from '../lib/labels'

const sortLabels: Record<FeedSort, string> = {
  RELEVANCE: 'Best match',
  RECENT: 'Newest',
  TRENDING: 'Trending',
  SUPPORTED: 'Most supported',
  IMPACT: 'High impact',
}

const publicStatuses = Object.keys(statusLabels).filter((s) => s !== 'SUBMITTED' && s !== 'UNDER_REVIEW')
const filterKeys = ['q', 'localityId', 'categoryId', 'status', 'urgency', 'assistance', 'sort'] as const

const selectClass = 'w-full rounded-md border border-stone-300 bg-white px-2 py-2 text-sm'

/** The public feed. All filters live in the URL, so a filtered view can be shared as a link. */
export function ExploreProblemsPage() {
  const [params, setParams] = useSearchParams()
  const filters: FeedFilters = Object.fromEntries(filterKeys.map((k) => [k, params.get(k) ?? undefined]))
  const [query, setQuery] = useState(filters.q ?? '')

  const categories = useCategories()
  const cities = useCities()
  const pilotCity = cities.data?.length === 1 ? cities.data[0].id : null
  const localities = useLocalities(pilotCity)
  const feed = useFeed(filters)

  function update(key: (typeof filterKeys)[number], value: string) {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key === 'q' && !value && next.get('sort') === 'RELEVANCE') next.delete('sort')
    setParams(next, { replace: true })
  }

  function search(e: FormEvent) {
    e.preventDefault()
    update('q', query.trim())
  }

  const items = feed.data?.pages.flatMap((p) => p.items) ?? []
  const sortOptions = (Object.keys(sortLabels) as FeedSort[]).filter((s) => s !== 'RELEVANCE' || filters.q)
  const activeFilters = filterKeys.filter((k) => k !== 'sort' && filters[k]).length

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-bold">Explore problems</h1>
        <p className="text-stone-600">Find problems near you, support them, and see where help is needed.</p>
      </div>

      <form onSubmit={search} role="search" className="flex gap-2">
        <label htmlFor="feed-search" className="sr-only">
          Search problems
        </label>
        <input
          id="feed-search"
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search, e.g. drainage Aliganj"
          className="flex-1 rounded-md border border-stone-300 bg-white px-3 py-2"
        />
        <Button type="submit">Search</Button>
      </form>

      <div className="grid gap-3 rounded-lg border border-stone-200 bg-white p-3 sm:grid-cols-3 lg:grid-cols-6">
        <label className="space-y-1 text-xs font-medium">
          <span>Locality</span>
          <select className={selectClass} value={filters.localityId ?? ''} onChange={(e) => update('localityId', e.target.value)}>
            <option value="">All localities</option>
            {localities.data?.map((l) => (
              <option key={l.id} value={l.id}>
                {l.name}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1 text-xs font-medium">
          <span>Category</span>
          <select className={selectClass} value={filters.categoryId ?? ''} onChange={(e) => update('categoryId', e.target.value)}>
            <option value="">All categories</option>
            {categories.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nameEn}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1 text-xs font-medium">
          <span>Status</span>
          <select className={selectClass} value={filters.status ?? ''} onChange={(e) => update('status', e.target.value)}>
            <option value="">Any status</option>
            {publicStatuses.map((s) => (
              <option key={s} value={s}>
                {statusLabels[s as keyof typeof statusLabels]}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1 text-xs font-medium">
          <span>Urgency</span>
          <select className={selectClass} value={filters.urgency ?? ''} onChange={(e) => update('urgency', e.target.value)}>
            <option value="">Any urgency</option>
            {Object.entries(urgencyLabels).map(([k, v]) => (
              <option key={k} value={k}>
                {v.split(' — ')[0]}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1 text-xs font-medium">
          <span>Help needed</span>
          <select className={selectClass} value={filters.assistance ?? ''} onChange={(e) => update('assistance', e.target.value)}>
            <option value="">Any kind</option>
            {Object.entries(assistanceLabels).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1 text-xs font-medium">
          <span>Sort by</span>
          <select
            className={selectClass}
            value={filters.sort ?? (filters.q ? 'RELEVANCE' : 'RECENT')}
            onChange={(e) => update('sort', e.target.value)}
          >
            {sortOptions.map((s) => (
              <option key={s} value={s}>
                {sortLabels[s]}
              </option>
            ))}
          </select>
        </label>
      </div>

      {activeFilters > 0 && (
        <button
          type="button"
          className="text-sm text-udaan-700 underline"
          onClick={() => {
            setQuery('')
            setParams(new URLSearchParams(), { replace: true })
          }}
        >
          Clear {activeFilters === 1 ? 'filter' : 'filters'}
        </button>
      )}

      {feed.isPending && <p className="text-stone-500">Loading problems… (the test server may be waking up)</p>}
      {feed.isError && <FormError message={errorMessage(feed.error)} />}
      {feed.isSuccess && items.length === 0 && (
        <p className="rounded-lg border border-dashed border-stone-300 p-6 text-center text-stone-600">
          No problems match these filters yet.
        </p>
      )}

      <ul className="grid gap-3 md:grid-cols-2">
        {items.map((p) => (
          <li key={p.id}>
            <ProblemCard problem={p} />
          </li>
        ))}
      </ul>

      {feed.hasNextPage && (
        <div className="text-center">
          <Button variant="secondary" busy={feed.isFetchingNextPage} onClick={() => void feed.fetchNextPage()}>
            Show more
          </Button>
        </div>
      )}
    </div>
  )
}
