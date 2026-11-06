import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Button, FormError, TextArea, TextField } from '../components/shared/Form'
import { useProblem } from '../features/problems/problemApi'
import { useProposeSolution } from '../features/solutions/solutionApi'
import { ApiError, errorMessage } from '../lib/api'

function validate(f: { title: string; summary: string; approach: string; cost: string }) {
  const e: Record<string, string> = {}
  if (f.title.trim().length < 10) e.title = 'Give a title of at least 10 characters.'
  if (f.summary.trim().length < 30) e.summary = 'Summarise the solution in at least 30 characters.'
  if (f.approach.trim().length < 30) e.approach = 'Explain how it would be done in at least 30 characters.'
  if (f.cost && !(Number(f.cost) >= 0)) e.estimatedCostInr = 'Enter an amount in rupees, or leave it blank.'
  return e
}

export function ProposeSolutionPage() {
  const { code = '' } = useParams()
  const navigate = useNavigate()
  const problem = useProblem(code)
  const propose = useProposeSolution(code)
  const [form, setForm] = useState({ title: '', summary: '', approach: '', cost: '', timeline: '', resources: '', skills: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const set = (k: keyof typeof form, v: string) => setForm((f) => ({ ...f, [k]: v }))

  function submit(e: FormEvent) {
    e.preventDefault()
    const found = validate(form)
    setErrors(found)
    if (Object.keys(found).length > 0) return
    propose.mutate(
      {
        title: form.title,
        summary: form.summary,
        approach: form.approach,
        estimatedCostInr: form.cost ? Number(form.cost) : null,
        timelineText: form.timeline,
        resourcesRequired: form.resources,
        skills: form.skills.split(',').map((s) => s.trim()).filter(Boolean),
      },
      { onSuccess: (s) => navigate(`/solutions/${s.id}`, { replace: true }) },
    )
  }

  const fieldError = (f: string) => errors[f] ?? errorMessage(propose.error, f)
  const general =
    Object.keys(errors).length > 0
      ? 'Please fix the highlighted fields.'
      : propose.isError && !(propose.error instanceof ApiError && propose.error.code === 'VALIDATION_FAILED')
        ? errorMessage(propose.error)
        : undefined

  return (
    <form onSubmit={submit} className="mx-auto max-w-2xl space-y-4" noValidate>
      <div className="space-y-1">
        <h1 className="text-2xl font-bold">Propose a solution</h1>
        {problem.data && (
          <p className="text-stone-600">
            For:{' '}
            <Link to={`/problems/${code}`} className="underline">
              {problem.data.title}
            </Link>
          </p>
        )}
      </div>
      <TextField label="Solution title" maxLength={150} value={form.title} onChange={(e) => set('title', e.target.value)} error={fieldError('title')} />
      <TextArea
        label="Summary"
        maxLength={2000}
        value={form.summary}
        onChange={(e) => set('summary', e.target.value)}
        error={fieldError('summary')}
        hint="What is the solution, in a few sentences?"
      />
      <TextArea
        label="How would it be done?"
        rows={6}
        maxLength={5000}
        value={form.approach}
        onChange={(e) => set('approach', e.target.value)}
        error={fieldError('approach')}
        hint="Steps, who does what, and anything that needs approval."
      />
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField
          label="Estimated cost in ₹ (optional)"
          type="number"
          min={0}
          inputMode="numeric"
          value={form.cost}
          onChange={(e) => set('cost', e.target.value)}
          error={fieldError('estimatedCostInr')}
        />
        <TextField label="Timeline (optional)" maxLength={200} placeholder="e.g. 3 weeks" value={form.timeline} onChange={(e) => set('timeline', e.target.value)} />
      </div>
      <TextArea label="Resources needed (optional)" maxLength={1000} rows={2} value={form.resources} onChange={(e) => set('resources', e.target.value)} />
      <TextField
        label="Skills needed (optional)"
        value={form.skills}
        onChange={(e) => set('skills', e.target.value)}
        hint="Separate with commas, e.g. civil work, fundraising."
      />
      <FormError message={general} />
      <Button type="submit" busy={propose.isPending}>
        Publish solution
      </Button>
    </form>
  )
}
