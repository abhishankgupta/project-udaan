import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { useSession } from '../lib/session'
import { authResponse, defaultHandlers, mockApi, problem, renderAt } from '../test/utils'

describe('Signing in with email', () => {
  it('signs in and shows the user in the header', async () => {
    const calls = mockApi({
      ...defaultHandlers,
      'POST /api/v1/auth/login': () => Response.json(authResponse('Asha Verma', { email: 'asha@example.org' })),
    })
    const user = userEvent.setup()
    const { router } = renderAt('/login')

    await user.click(await screen.findByRole('tab', { name: 'Email' }))
    await user.type(screen.getByLabelText('Email'), 'asha@example.org')
    await user.type(screen.getByLabelText('Password'), 'correct-horse-1')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('link', { name: 'Asha Verma' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/')
    expect(calls.find((c) => c.key === 'POST /api/v1/auth/login')?.body).toEqual({
      email: 'asha@example.org',
      password: 'correct-horse-1',
    })
    expect(localStorage.getItem('udaan.refreshToken')).toBe('refresh-1')
  })

  it('shows the error from the API when the password is wrong', async () => {
    mockApi({
      ...defaultHandlers,
      'POST /api/v1/auth/login': () => problem(401, 'INVALID_CREDENTIALS', 'Email or password is incorrect'),
    })
    const user = userEvent.setup()
    renderAt('/login')

    await user.click(await screen.findByRole('tab', { name: 'Email' }))
    await user.type(screen.getByLabelText('Email'), 'asha@example.org')
    await user.type(screen.getByLabelText('Password'), 'nope-nope')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Email or password is incorrect')
    expect(useSession.getState().user).toBeNull()
  })
})

describe('Signing in with a mobile number', () => {
  it('shows the test-mode code, verifies it and returns to the page the user wanted', async () => {
    const calls = mockApi({
      ...defaultHandlers,
      'POST /api/v1/auth/otp/request': () => Response.json({ phone: '+91 ••••• •3210', expiresInSeconds: 300, testCode: '482913' }),
      'POST /api/v1/auth/otp/verify': () => Response.json(authResponse('Ravi', { phone: '+919876543210' })),
    })
    const user = userEvent.setup()
    const { router } = renderAt('/login?next=%2Fme')

    await user.type(await screen.findByLabelText('Mobile number'), '98765 43210')
    await user.click(screen.getByRole('button', { name: 'Send code' }))

    expect(await screen.findByText('482913')).toBeInTheDocument()
    expect(screen.getByText(/Enter the code sent to \+91 ••••• •3210/)).toBeInTheDocument()

    await user.type(screen.getByLabelText('6-digit code'), '482913')
    await user.type(screen.getByLabelText(/Your name/), 'Ravi')
    await user.click(screen.getByRole('button', { name: 'Verify and sign in' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/me'))
    expect(calls.find((c) => c.key === 'POST /api/v1/auth/otp/verify')?.body).toEqual({
      phone: '98765 43210',
      code: '482913',
      displayName: 'Ravi',
    })
  })

  it('hides the phone option when the API says it is unavailable', async () => {
    mockApi({
      ...defaultHandlers,
      'GET /api/v1/auth/options': () => Response.json({ emailEnabled: true, phoneOtpEnabled: false, phoneOtpTestMode: false }),
    })
    renderAt('/login')

    expect(await screen.findByLabelText('Email')).toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Mobile number' })).not.toBeInTheDocument()
  })
})

describe('Registering', () => {
  it('checks password length before calling the API', async () => {
    const calls = mockApi(defaultHandlers)
    const user = userEvent.setup()
    renderAt('/register')

    await user.type(screen.getByLabelText('Your name'), 'Asha')
    await user.type(screen.getByLabelText('Email'), 'asha@example.org')
    await user.type(screen.getByLabelText('Password'), 'short')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('at least 8 characters')
    expect(calls.some((c) => c.key === 'POST /api/v1/auth/register')).toBe(false)
  })

  it('shows field errors returned by the API', async () => {
    mockApi({
      ...defaultHandlers,
      'POST /api/v1/auth/register': () =>
        problem(400, 'VALIDATION_FAILED', 'Request has invalid fields', {
          fieldErrors: [{ field: 'email', message: 'must be a well-formed email address' }],
        }),
    })
    const user = userEvent.setup()
    renderAt('/register')

    await user.type(screen.getByLabelText('Your name'), 'Asha')
    await user.type(screen.getByLabelText('Email'), 'not-an-email')
    await user.type(screen.getByLabelText('Password'), 'long-enough-1')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument()
  })
})

describe('Sessions', () => {
  it('sends signed-out visitors to sign in, keeping where they were going', async () => {
    mockApi(defaultHandlers)
    const { router } = renderAt('/problems/new')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.search).toBe('?next=%2Fproblems%2Fnew')
  })

  it('restores a returning visitor from the stored refresh token', async () => {
    const calls = mockApi({
      ...defaultHandlers,
      'POST /api/v1/auth/refresh': () => Response.json(authResponse('Returning User')),
    })
    useSession.setState({ refreshToken: 'stored-refresh', restoring: true })
    renderAt('/')

    expect(await screen.findByRole('link', { name: 'Returning User' })).toBeInTheDocument()
    expect(calls.find((c) => c.key === 'POST /api/v1/auth/refresh')?.body).toEqual({ refreshToken: 'stored-refresh' })
  })

  it('refreshes an expired access token once and retries the request', async () => {
    let meCalls = 0
    mockApi({
      ...defaultHandlers,
      'POST /api/v1/auth/refresh': () => Response.json({ ...authResponse('Asha'), accessToken: 'access-2' }),
      'GET /api/v1/me': (_body, init) => {
        meCalls++
        const auth = (init.headers as Record<string, string>).Authorization
        return auth === 'Bearer access-2'
          ? Response.json({ id: 'u1', displayName: 'Asha', email: 'a@example.org', phone: null, roles: ['INDIVIDUAL'], bio: null, city: null, locality: null, skills: [], memberSince: '2026-11-01T00:00:00Z' })
          : problem(401, 'UNAUTHORIZED', 'expired')
      },
    })
    useSession.setState({
      user: authResponse('Asha').user,
      accessToken: 'access-expired',
      refreshToken: 'refresh-1',
      restoring: false,
    })
    renderAt('/me')

    expect(await screen.findByDisplayValue('Asha')).toBeInTheDocument()
    expect(meCalls).toBe(2)
    expect(useSession.getState().accessToken).toBe('access-2')
  })

  it('signs out', async () => {
    const calls = mockApi({ ...defaultHandlers, 'POST /api/v1/auth/logout': () => new Response(null, { status: 204 }) })
    useSession.setState({ user: authResponse('Asha').user, accessToken: 'a', refreshToken: 'refresh-1', restoring: false })
    const user = userEvent.setup()
    renderAt('/')

    await user.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(calls.some((c) => c.key === 'POST /api/v1/auth/logout')).toBe(true)
    expect(localStorage.getItem('udaan.refreshToken')).toBeNull()
  })
})

describe('Profile', () => {
  it('saves profile changes and updates the name in the header', async () => {
    const saved: unknown[] = []
    const base = { id: 'u1', email: 'a@example.org', phone: null, roles: ['INDIVIDUAL'], memberSince: '2026-11-01T00:00:00Z' }
    mockApi({
      ...defaultHandlers,
      'GET /api/v1/me': () => Response.json({ ...base, displayName: 'Asha', bio: null, city: null, locality: null, skills: [] }),
      'PATCH /api/v1/me': (body) => {
        saved.push(body)
        return Response.json({ ...base, ...(body as object) })
      },
    })
    useSession.setState({ user: authResponse('Asha').user, accessToken: 'a', refreshToken: 'r', restoring: false })
    const user = userEvent.setup()
    renderAt('/me')

    const name = await screen.findByLabelText('Name')
    await user.clear(name)
    await user.type(name, 'Asha Verma')
    await user.type(screen.getByLabelText('City'), 'Lucknow')
    await user.type(screen.getByLabelText('Skills'), 'Drainage, Roads')
    await user.click(screen.getByRole('button', { name: 'Save profile' }))

    expect(await screen.findByText('Profile saved.')).toBeInTheDocument()
    expect(saved[0]).toEqual({ displayName: 'Asha Verma', bio: '', city: 'Lucknow', locality: '', skills: ['Drainage', 'Roads'] })
    expect(screen.getByRole('link', { name: 'Asha Verma' })).toBeInTheDocument()
    expect(screen.getByText('a@example.org')).toBeInTheDocument()
  })
})
