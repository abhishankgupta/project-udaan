import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { Button, FormError, TextField } from '../components/shared/Form'
import { authApi, useAuthOptions, type OtpIssued } from '../features/auth/authApi'
import { errorMessage } from '../lib/api'
import { safeNext } from '../features/auth/safeNext'
import { useSession, type AuthResponse } from '../lib/session'

function useFinishSignIn() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const signIn = useSession((s) => s.signIn)
  return (auth: AuthResponse) => {
    signIn(auth)
    navigate(safeNext(params.get('next')), { replace: true })
  }
}

export function LoginPage() {
  const options = useAuthOptions()
  const phoneAvailable = options.data?.phoneOtpEnabled ?? false
  const [method, setMethod] = useState<'phone' | 'email' | null>(null)
  const active = method ?? (phoneAvailable ? 'phone' : 'email')
  const [params] = useSearchParams()

  return (
    <div className="mx-auto max-w-md space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-bold">Sign in to Udaan</h1>
        <p className="text-stone-600">Raise problems, support others and propose solutions.</p>
      </div>

      {phoneAvailable && (
        <div role="tablist" aria-label="Sign-in method" className="grid grid-cols-2 rounded-md border border-stone-300 p-1">
          {(['phone', 'email'] as const).map((m) => (
            <button
              key={m}
              role="tab"
              aria-selected={active === m}
              onClick={() => setMethod(m)}
              className={`rounded px-3 py-2 text-sm font-medium ${active === m ? 'bg-udaan-600 text-white' : 'text-stone-700'}`}
            >
              {m === 'phone' ? 'Mobile number' : 'Email'}
            </button>
          ))}
        </div>
      )}

      {active === 'phone' && phoneAvailable ? (
        <PhoneSignIn testMode={options.data?.phoneOtpTestMode ?? false} />
      ) : (
        <EmailSignIn />
      )}

      <p className="text-sm text-stone-600">
        New here?{' '}
        <Link to={`/register${params.get('next') ? `?next=${encodeURIComponent(params.get('next')!)}` : ''}`} className="font-medium text-udaan-700 underline">
          Create an account with email
        </Link>
        {phoneAvailable && ' — or just sign in with your mobile number; we create your account automatically.'}
      </p>
    </div>
  )
}

function EmailSignIn() {
  const finish = useFinishSignIn()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const login = useMutation({ mutationFn: authApi.login, onSuccess: finish })

  function submit(e: FormEvent) {
    e.preventDefault()
    login.mutate({ email, password })
  }

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <FormError message={login.isError ? errorMessage(login.error) : undefined} />
      <TextField label="Email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
      <TextField label="Password" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
      <Button type="submit" busy={login.isPending} className="w-full">
        Sign in
      </Button>
    </form>
  )
}

function PhoneSignIn({ testMode }: { testMode: boolean }) {
  const finish = useFinishSignIn()
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [issued, setIssued] = useState<OtpIssued | null>(null)

  const request = useMutation({ mutationFn: authApi.requestOtp, onSuccess: setIssued })
  const verify = useMutation({ mutationFn: authApi.verifyOtp, onSuccess: finish })

  if (!issued) {
    return (
      <form
        onSubmit={(e) => {
          e.preventDefault()
          request.mutate(phone)
        }}
        className="space-y-4"
        noValidate
      >
        <FormError message={request.isError ? errorMessage(request.error) : undefined} />
        <TextField
          label="Mobile number"
          type="tel"
          inputMode="tel"
          autoComplete="tel"
          placeholder="98765 43210"
          required
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          hint="Indian mobile numbers only. We'll send a 6-digit code."
        />
        <Button type="submit" busy={request.isPending} className="w-full">
          Send code
        </Button>
      </form>
    )
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        verify.mutate({ phone, code, displayName: name || undefined })
      }}
      className="space-y-4"
      noValidate
    >
      {testMode && issued.testCode && (
        <p className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm">
          Test mode: SMS isn't connected yet, so here is your code: <strong className="font-mono text-base">{issued.testCode}</strong>
        </p>
      )}
      <p className="text-sm text-stone-600">
        Enter the code sent to {issued.phone}.{' '}
        <button type="button" className="text-udaan-700 underline" onClick={() => { setIssued(null); setCode(''); verify.reset() }}>
          Change number
        </button>
      </p>
      <FormError message={verify.isError ? errorMessage(verify.error) : undefined} />
      <TextField
        label="6-digit code"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        required
        value={code}
        onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
      />
      <TextField
        label="Your name (for new accounts)"
        autoComplete="name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        hint="Shown on problems you raise. You can change it later."
      />
      <Button type="submit" busy={verify.isPending} className="w-full">
        Verify and sign in
      </Button>
    </form>
  )
}
