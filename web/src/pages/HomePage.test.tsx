import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { defaultHandlers, mockApi, renderAt } from '../test/utils'

describe('HomePage', () => {
  it('shows categories and the API build from the backend', async () => {
    mockApi({
      ...defaultHandlers,
      'GET /api/v1/categories': () =>
        Response.json([
          { id: '1', code: 'DRAINAGE_WATER', nameEn: 'Drainage and water', nameHi: 'जल निकासी और पानी' },
          { id: '2', code: 'EDUCATION', nameEn: 'Education', nameHi: 'शिक्षा' },
        ]),
    })
    renderAt('/')

    expect(await screen.findByText('Drainage and water')).toBeInTheDocument()
    expect(screen.getByText('शिक्षा')).toBeInTheDocument()
    expect(await screen.findByText(/build abcdef1/)).toBeInTheDocument()
  })

  it('shows the non-production banner, the emergency notice and a sign-in link', async () => {
    mockApi(defaultHandlers)
    renderAt('/')

    expect(screen.getByRole('status')).toHaveTextContent(/environment/i)
    expect(screen.getByText(/In an emergency, call 112/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('shows an error when the API is down', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 503 }))
    renderAt('/')

    expect(await screen.findByText('Could not load categories.')).toBeInTheDocument()
  })
})
