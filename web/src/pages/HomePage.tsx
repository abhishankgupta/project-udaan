import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { Button } from '../components/shared/Form'
import { useApiMeta } from '../features/meta/useApiMeta'
import { ProblemCard } from '../features/problems/ProblemCard'
import { useFeed, type FeedSort } from '../features/problems/problemApi'
import { useCategories } from '../features/taxonomy/useCategories'

function ProblemSection({ title, sort, emptyText }: { title: string; sort: FeedSort; emptyText: string }) {
  const feed = useFeed({ sort }, 4)
  const items = feed.data?.pages[0]?.items ?? []
  return (
    <section className="space-y-3" aria-label={title}>
      <div className="flex items-baseline justify-between gap-2">
        <h2 className="text-xl font-semibold">{title}</h2>
        <Link to={`/problems?sort=${sort}`} className="text-sm text-udaan-700 underline">
          See all
        </Link>
      </div>
      {feed.isPending && <p className="text-sm text-stone-500">Loading…</p>}
      {feed.isSuccess && items.length === 0 && <p className="text-sm text-stone-500">{emptyText}</p>}
      <ul className="grid gap-3 md:grid-cols-2">
        {items.map((p) => (
          <li key={p.id}>
            <ProblemCard problem={p} />
          </li>
        ))}
      </ul>
    </section>
  )
}

export function HomePage() {
  const meta = useApiMeta()
  const categories = useCategories()
  const navigate = useNavigate()
  const [query, setQuery] = useState('')

  function search(e: FormEvent) {
    e.preventDefault()
    navigate(query.trim() ? `/problems?q=${encodeURIComponent(query.trim())}` : '/problems')
  }

  return (
    <div className="space-y-10">
      <section className="space-y-4">
        <h1 className="text-3xl font-bold sm:text-4xl">Every problem deserves visibility.</h1>
        <p className="max-w-2xl text-lg text-stone-600">
          Raise a problem in your area, and connect with citizens, NGOs, companies and authorities who can help solve it.
        </p>
        <form onSubmit={search} role="search" className="flex max-w-xl gap-2">
          <label htmlFor="home-search" className="sr-only">
            Search problems
          </label>
          <input
            id="home-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search problems by keyword or locality"
            className="flex-1 rounded-md border border-stone-300 bg-white px-3 py-2"
          />
          <Button type="submit">Search</Button>
        </form>
      </section>

      <ProblemSection title="Trending problems" sort="TRENDING" emptyText="No problems yet. Be the first to raise one." />
      <ProblemSection title="High-impact challenges" sort="IMPACT" emptyText="Nothing here yet." />
      <ProblemSection title="Recently raised" sort="RECENT" emptyText="Nothing here yet." />

      <section aria-labelledby="categories-heading" className="space-y-3">
        <h2 id="categories-heading" className="text-xl font-semibold">
          Browse by category
        </h2>
        {categories.isPending && <p className="text-stone-500">Loading categories… (the test server may be waking up)</p>}
        {categories.isError && <p className="text-red-700">Could not load categories.</p>}
        {categories.data && (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {categories.data.map((c) => (
              <li key={c.id}>
                <Link to={`/problems?categoryId=${c.id}`} className="block rounded-lg border border-stone-200 bg-white p-4 hover:border-udaan-600">
                  <p className="font-medium">{c.nameEn}</p>
                  <p lang="hi" className="text-sm text-stone-500">
                    {c.nameHi}
                  </p>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-label="System status" className="text-xs text-stone-500">
        {meta.data ? (
          <p>
            API: {meta.data.environment} · build {meta.data.version.slice(0, 7)}
          </p>
        ) : meta.isError ? (
          <p>API: unreachable</p>
        ) : (
          <p>API: connecting…</p>
        )}
      </section>
    </div>
  )
}
