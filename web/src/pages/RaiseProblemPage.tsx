import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { Button, FormError, TextArea, TextField } from '../components/shared/Form'
import { useCities, useLocalities } from '../features/geo/geoApi'
import { PhotoPicker } from '../features/problems/PhotoPicker'
import { SimilarProblems } from '../features/problems/SimilarProblems'
import { useSubmitProblem, type Photo } from '../features/problems/problemApi'
import { useCategories } from '../features/taxonomy/useCategories'
import { ApiError, errorMessage } from '../lib/api'
import { assistanceLabels, urgencyLabels, type AssistanceType, type Urgency } from '../lib/labels'

const DRAFT_KEY = 'udaan.problemDraft'
const OTHER = 'other'

interface FormState {
  title: string
  description: string
  categoryId: string
  cityId: string
  locality: string // a locality id, OTHER, or ''
  localityText: string
  addressText: string
  latitude: number | null
  longitude: number | null
  urgency: Urgency | ''
  peopleAffected: string
  estimatedImpact: string
  desiredOutcome: string
  assistanceTypes: AssistanceType[]
  anonymous: boolean
}

const emptyForm: FormState = {
  title: '',
  description: '',
  categoryId: '',
  cityId: '',
  locality: '',
  localityText: '',
  addressText: '',
  latitude: null,
  longitude: null,
  urgency: '',
  peopleAffected: '',
  estimatedImpact: '',
  desiredOutcome: '',
  assistanceTypes: [],
  anonymous: false,
}

function loadDraft(): FormState {
  try {
    const saved = localStorage.getItem(DRAFT_KEY)
    return saved ? { ...emptyForm, ...(JSON.parse(saved) as Partial<FormState>) } : emptyForm
  } catch {
    return emptyForm
  }
}

function saveDraft(form: FormState) {
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(form))
  } catch {
    // Drafts are a convenience; ignore storage failures.
  }
}

function clearDraft() {
  try {
    localStorage.removeItem(DRAFT_KEY)
  } catch {
    // ignore
  }
}

/** Client-side checks that mirror the API's rules, so most mistakes show before submitting. */
function validate(form: FormState): Record<string, string> {
  const errors: Record<string, string> = {}
  if (form.title.trim().length < 10) errors.title = 'Give a title of at least 10 characters.'
  if (form.description.trim().length < 30) errors.description = 'Describe the problem in at least 30 characters.'
  if (!form.categoryId) errors.categoryId = 'Choose a category.'
  if (!form.cityId) errors.cityId = 'Choose a city.'
  if (!form.locality || (form.locality === OTHER && !form.localityText.trim())) errors.localityId = 'Choose or type your locality.'
  if (!form.urgency) errors.urgency = 'Choose how urgent this is.'
  if (form.desiredOutcome.trim().length < 10) errors.desiredOutcome = 'Describe the outcome you want in at least 10 characters.'
  if (form.assistanceTypes.length === 0) errors.assistanceTypes = 'Choose at least one kind of help.'
  if (form.peopleAffected && !(Number(form.peopleAffected) >= 1)) errors.peopleAffected = 'Enter a number, or leave it blank.'
  return errors
}

function Section({ step, title, children }: { step: number; title: string; children: ReactNode }) {
  return (
    <fieldset className="space-y-4 rounded-lg border border-stone-200 bg-white p-4 sm:p-6">
      <legend className="px-1 text-lg font-semibold">
        <span className="mr-2 text-udaan-700">{step}.</span>
        {title}
      </legend>
      {children}
    </fieldset>
  )
}

export function RaiseProblemPage() {
  const navigate = useNavigate()
  const categories = useCategories()
  const cities = useCities()
  const [form, setForm] = useState<FormState>(loadDraft)
  const [photos, setPhotos] = useState<Photo[]>([])
  const [photosBusy, setPhotosBusy] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [locating, setLocating] = useState(false)
  const [locationError, setLocationError] = useState<string>()
  const submit = useSubmitProblem()

  // During the pilot there is one city, so it is pre-selected.
  const cityValue = form.cityId || (cities.data?.length === 1 ? String(cities.data[0].id) : '')
  const cityId = cityValue ? Number(cityValue) : null
  const localities = useLocalities(cityId)

  useEffect(() => saveDraft(form), [form])

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((f) => ({ ...f, [key]: value }))

  function requestLocation() {
    if (!('geolocation' in navigator)) {
      setLocationError('Your browser cannot share its location.')
      return
    }
    setLocating(true)
    setLocationError(undefined)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setForm((f) => ({
          ...f,
          latitude: Number(pos.coords.latitude.toFixed(6)),
          longitude: Number(pos.coords.longitude.toFixed(6)),
        }))
        setLocating(false)
      },
      () => {
        setLocationError('Could not get your location. You can skip this.')
        setLocating(false)
      },
      { enableHighAccuracy: true, timeout: 15000 },
    )
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const found = validate({ ...form, cityId: cityValue })
    setErrors(found)
    if (Object.keys(found).length > 0) return
    submit.mutate(
      {
        title: form.title,
        description: form.description,
        categoryId: form.categoryId,
        cityId: Number(cityValue),
        localityId: form.locality === OTHER ? null : Number(form.locality),
        localityText: form.locality === OTHER ? form.localityText : null,
        addressText: form.addressText,
        latitude: form.latitude,
        longitude: form.longitude,
        urgency: form.urgency as Urgency,
        peopleAffected: form.peopleAffected ? Number(form.peopleAffected) : null,
        estimatedImpact: form.estimatedImpact,
        desiredOutcome: form.desiredOutcome,
        assistanceTypes: form.assistanceTypes,
        anonymous: form.anonymous,
        mediaIds: photos.map((p) => p.id),
      },
      {
        onSuccess: (problem) => {
          clearDraft()
          navigate(`/problems/${problem.publicCode}`, { replace: true, state: { justSubmitted: true } })
        },
      },
    )
  }

  const fieldError = (field: string) => errors[field] ?? errorMessage(submit.error, field)
  const serverGeneral =
    submit.isError && !(submit.error instanceof ApiError && submit.error.code === 'VALIDATION_FAILED')
      ? errorMessage(submit.error)
      : undefined
  const hasErrors = Object.keys(errors).length > 0

  return (
    <form onSubmit={onSubmit} className="mx-auto max-w-2xl space-y-6" noValidate>
      <div className="space-y-1">
        <h1 className="text-2xl font-bold">Raise a problem</h1>
        <p className="text-stone-600">
          Describe a problem in your area so others can support it and help solve it. Your draft is saved on this device as you type.
        </p>
      </div>

      <Section step={1} title="What's the problem?">
        <TextField
          label="Title"
          maxLength={150}
          placeholder="e.g. Waterlogging on Station Road after every rain"
          value={form.title}
          onChange={(e) => set('title', e.target.value)}
          error={fieldError('title')}
        />
        <div className="space-y-1">
          <label htmlFor="category" className="block text-sm font-medium">
            Category
          </label>
          <select
            id="category"
            value={form.categoryId}
            onChange={(e) => set('categoryId', e.target.value)}
            aria-invalid={fieldError('categoryId') ? true : undefined}
            className="w-full rounded-md border border-stone-300 bg-white px-3 py-2"
          >
            <option value="">Choose a category</option>
            {categories.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nameEn} · {c.nameHi}
              </option>
            ))}
          </select>
          {fieldError('categoryId') && <p className="text-sm text-red-700">{fieldError('categoryId')}</p>}
        </div>
        <TextArea
          label="Description"
          rows={5}
          maxLength={5000}
          placeholder="What is happening, since when, and who is affected?"
          value={form.description}
          onChange={(e) => set('description', e.target.value)}
          error={fieldError('description')}
        />
      </Section>

      <Section step={2} title="Where is it?">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1">
            <label htmlFor="city" className="block text-sm font-medium">
              City
            </label>
            <select
              id="city"
              value={cityValue}
              onChange={(e) => setForm((f) => ({ ...f, cityId: e.target.value, locality: '' }))}
              className="w-full rounded-md border border-stone-300 bg-white px-3 py-2"
            >
              <option value="">Choose a city</option>
              {cities.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}, {c.stateName}
                </option>
              ))}
            </select>
            {fieldError('cityId') && <p className="text-sm text-red-700">{fieldError('cityId')}</p>}
            <p className="text-xs text-stone-500">Udaan is starting in Lucknow; more cities will follow.</p>
          </div>
          <div className="space-y-1">
            <label htmlFor="locality" className="block text-sm font-medium">
              Locality
            </label>
            <select
              id="locality"
              value={form.locality}
              disabled={!cityId}
              onChange={(e) => set('locality', e.target.value)}
              aria-invalid={fieldError('localityId') ? true : undefined}
              className="w-full rounded-md border border-stone-300 bg-white px-3 py-2 disabled:bg-stone-100"
            >
              <option value="">Choose a locality</option>
              {localities.data?.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.name}
                </option>
              ))}
              <option value={OTHER}>Other (type it)</option>
            </select>
            {fieldError('localityId') && <p className="text-sm text-red-700">{fieldError('localityId')}</p>}
          </div>
        </div>
        {form.locality === OTHER && (
          <TextField label="Locality name" maxLength={120} value={form.localityText} onChange={(e) => set('localityText', e.target.value)} />
        )}
        <TextField
          label="Landmark or address (optional)"
          maxLength={200}
          placeholder="e.g. Near the post office, Sector H"
          value={form.addressText}
          onChange={(e) => set('addressText', e.target.value)}
        />
        <div className="space-y-1 text-sm">
          {form.latitude !== null && form.longitude !== null ? (
            <p>
              Exact spot added: {form.latitude}, {form.longitude}{' '}
              <button type="button" className="text-udaan-700 underline" onClick={() => setForm((f) => ({ ...f, latitude: null, longitude: null }))}>
                Remove
              </button>
            </p>
          ) : (
            <Button type="button" variant="secondary" busy={locating} onClick={requestLocation}>
              Use my current location (optional)
            </Button>
          )}
          {locationError && <p className="text-red-700">{locationError}</p>}
        </div>
      </Section>

      <SimilarProblems
        title={form.title}
        cityId={cityId}
        localityId={form.locality && form.locality !== OTHER ? Number(form.locality) : null}
      />

      <Section step={3} title="Photos">
        <PhotoPicker photos={photos} onChange={setPhotos} onBusyChange={setPhotosBusy} />
      </Section>

      <Section step={4} title="What help is needed?">
        <div className="space-y-2">
          <p className="text-sm font-medium" id="urgency-label">
            How urgent is it?
          </p>
          <div role="radiogroup" aria-labelledby="urgency-label" className="grid gap-2 sm:grid-cols-2">
            {(Object.keys(urgencyLabels) as Urgency[]).map((u) => (
              <label key={u} className="flex items-center gap-2 rounded-md border border-stone-300 bg-white px-3 py-2 text-sm">
                <input type="radio" name="urgency" value={u} checked={form.urgency === u} onChange={() => set('urgency', u)} />
                {urgencyLabels[u]}
              </label>
            ))}
          </div>
          {fieldError('urgency') && <p className="text-sm text-red-700">{fieldError('urgency')}</p>}
          {form.urgency === 'CRITICAL' && (
            <p role="note" className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-900">
              If anyone is in danger right now, call <strong>112</strong> (emergency), <strong>108</strong> (ambulance) or{' '}
              <strong>101</strong> (fire). Udaan is not an emergency service.
            </p>
          )}
        </div>

        <div className="space-y-2">
          <p className="text-sm font-medium" id="help-label">
            Kinds of help needed
          </p>
          <div role="group" aria-labelledby="help-label" className="grid gap-2 sm:grid-cols-2">
            {(Object.keys(assistanceLabels) as AssistanceType[]).map((a) => (
              <label key={a} className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.assistanceTypes.includes(a)}
                  onChange={(e) =>
                    set('assistanceTypes', e.target.checked ? [...form.assistanceTypes, a] : form.assistanceTypes.filter((x) => x !== a))
                  }
                />
                {assistanceLabels[a]}
              </label>
            ))}
          </div>
          {fieldError('assistanceTypes') && <p className="text-sm text-red-700">{fieldError('assistanceTypes')}</p>}
        </div>

        <TextArea
          label="What outcome do you want?"
          maxLength={500}
          rows={2}
          placeholder="e.g. Working drains before the next monsoon"
          value={form.desiredOutcome}
          onChange={(e) => set('desiredOutcome', e.target.value)}
          error={fieldError('desiredOutcome')}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label="People affected (optional)"
            type="number"
            min={1}
            inputMode="numeric"
            value={form.peopleAffected}
            onChange={(e) => set('peopleAffected', e.target.value)}
            error={fieldError('peopleAffected')}
          />
          <TextField
            label="Impact (optional)"
            maxLength={500}
            placeholder="e.g. Children miss school"
            value={form.estimatedImpact}
            onChange={(e) => set('estimatedImpact', e.target.value)}
          />
        </div>
      </Section>

      <div className="space-y-4">
        <label className="flex items-start gap-2 text-sm">
          <input type="checkbox" className="mt-1" checked={form.anonymous} onChange={(e) => set('anonymous', e.target.checked)} />
          <span>
            Post anonymously. Your name won't be shown publicly; moderators can still see who raised it.
          </span>
        </label>
        <p className="text-xs text-stone-500">
          Please don't include phone numbers or accusations against named people. New problems are checked by moderators before
          they appear publicly.
        </p>
        <FormError message={hasErrors ? 'Please fix the highlighted fields.' : serverGeneral} />
        <Button type="submit" busy={submit.isPending} disabled={photosBusy}>
          {photosBusy ? 'Waiting for photos…' : 'Submit problem'}
        </Button>
      </div>
    </form>
  )
}
