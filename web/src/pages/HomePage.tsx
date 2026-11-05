import { useApiMeta } from '../features/meta/useApiMeta'
import { useCategories } from '../features/taxonomy/useCategories'

export function HomePage() {
  const meta = useApiMeta()
  const categories = useCategories()

  return (
    <div className="space-y-10">
      <section className="space-y-3">
        <h1 className="text-3xl font-bold sm:text-4xl">Every problem deserves visibility.</h1>
        <p className="max-w-2xl text-lg text-stone-600">
          Raise a problem in your locality, and connect with citizens, NGOs, companies and authorities who can
          help solve it.
        </p>
      </section>

      <section aria-labelledby="categories-heading" className="space-y-3">
        <h2 id="categories-heading" className="text-xl font-semibold">
          Browse by category
        </h2>
        {categories.isPending && <p className="text-stone-500">Loading categories… (the test server may be waking up)</p>}
        {categories.isError && <p className="text-red-700">Could not load categories.</p>}
        {categories.data && (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {categories.data.map((c) => (
              <li key={c.id} className="rounded-lg border border-stone-200 bg-white p-4">
                <p className="font-medium">{c.nameEn}</p>
                <p lang="hi" className="text-sm text-stone-500">
                  {c.nameHi}
                </p>
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
