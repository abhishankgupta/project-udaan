import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { Button, FormError, TextField } from '../components/shared/Form'
import { authApi } from '../features/auth/authApi'
import { ApiError, errorMessage } from '../lib/api'
import { useSession } from '../lib/session'
import { safeNext } from '../features/auth/safeNext'

export function RegisterPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const signIn = useSession((s) => s.signIn)
  const [form, setForm] = useState({ displayName: '', email: '', password: '' })
  const [localError, setLocalError] = useState<string>()

  const register = useMutation({
    mutationFn: authApi.register,
    onSuccess: (auth) => {
      signIn(auth)
      navigate(safeNext(params.get('next')), { replace: true })
    },
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    if (form.password.length < 8) {
      setLocalError('Password must be at least 8 characters.')
      return
    }
    setLocalError(undefined)
    register.mutate(form)
  }

  const fieldErrors = register.error instanceof ApiError && register.error.code === 'VALIDATION_FAILED'
  const generalError = localError ?? (register.isError && !fieldErrors ? errorMessage(register.error) : undefined)

  return (
    <div className="mx-auto max-w-md space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-bold">Create your account</h1>
        <p className="text-stone-600">It's free, and always will be for citizens.</p>
      </div>
      <form onSubmit={submit} className="space-y-4" noValidate>
        <FormError message={generalError} />
        <TextField
          label="Your name"
          autoComplete="name"
          required
          value={form.displayName}
          onChange={(e) => setForm({ ...form, displayName: e.target.value })}
          error={errorMessage(register.error, 'displayName')}
        />
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          required
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          error={errorMessage(register.error, 'email')}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          required
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
          error={errorMessage(register.error, 'password')}
          hint="At least 8 characters."
        />
        <Button type="submit" busy={register.isPending} className="w-full">
          Create account
        </Button>
      </form>
      <p className="text-sm text-stone-600">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-udaan-700 underline">
          Sign in
        </Link>
      </p>
    </div>
  )
}
