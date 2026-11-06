import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { Button, FormError, TextArea, TextField } from '../components/shared/Form'
import { useMyProfile, useUpdateMyProfile, type MyProfile } from '../features/profile/profileApi'
import { errorMessage } from '../lib/api'

export function MyProfilePage() {
  const profile = useMyProfile()

  if (profile.isPending) return <p className="text-stone-500">Loading your profile…</p>
  if (profile.isError) return <FormError message={errorMessage(profile.error)} />

  return <ProfileForm key={profile.data.id} profile={profile.data} />
}

function ProfileForm({ profile }: { profile: MyProfile }) {
  const update = useUpdateMyProfile()
  const [form, setForm] = useState({
    displayName: profile.displayName,
    bio: profile.bio ?? '',
    city: profile.city ?? '',
    locality: profile.locality ?? '',
    skills: profile.skills.join(', '),
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    update.mutate({
      ...form,
      skills: form.skills.split(',').map((s) => s.trim()).filter(Boolean),
    })
  }

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="text-2xl font-bold">Your profile</h1>
        <Link to={`/users/${profile.id}`} className="text-sm text-udaan-700 underline">
          See your public profile
        </Link>
      </div>

      <section className="rounded-lg border border-stone-200 bg-white p-4 text-sm">
        <h2 className="mb-2 font-semibold">Sign-in details (private)</h2>
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
          {profile.email && (
            <>
              <dt className="text-stone-500">Email</dt>
              <dd>{profile.email}</dd>
            </>
          )}
          {profile.phone && (
            <>
              <dt className="text-stone-500">Mobile</dt>
              <dd>{profile.phone}</dd>
            </>
          )}
        </dl>
        <p className="mt-2 text-xs text-stone-500">Never shown to other people.</p>
      </section>

      <form onSubmit={submit} className="space-y-4" noValidate>
        {update.isSuccess && (
          <p role="status" className="rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-800">
            Profile saved.
          </p>
        )}
        <FormError message={update.isError ? errorMessage(update.error) : undefined} />
        <TextField
          label="Name"
          required
          value={form.displayName}
          onChange={(e) => setForm({ ...form, displayName: e.target.value })}
          error={errorMessage(update.error, 'displayName')}
        />
        <TextArea
          label="About you"
          maxLength={500}
          value={form.bio}
          onChange={(e) => setForm({ ...form, bio: e.target.value })}
          hint="For example, your profession or how you'd like to help."
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField label="City" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
          <TextField label="Locality" value={form.locality} onChange={(e) => setForm({ ...form, locality: e.target.value })} />
        </div>
        <TextField
          label="Skills"
          value={form.skills}
          onChange={(e) => setForm({ ...form, skills: e.target.value })}
          hint="Separate with commas, e.g. civil engineering, legal advice, design."
        />
        <Button type="submit" busy={update.isPending}>
          Save profile
        </Button>
      </form>
    </div>
  )
}
