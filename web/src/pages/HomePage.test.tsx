import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { routes } from '../app/router'

function renderAt(path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

function mockApi() {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = String(input)
    if (url.endsWith('/api/v1/categories')) {
      return Response.json([
        { id: '1', code: 'DRAINAGE_WATER', nameEn: 'Drainage and water', nameHi: 'जल निकासी और पानी' },
        { id: '2', code: 'EDUCATION', nameEn: 'Education', nameHi: 'शिक्षा' },
      ])
    }
    if (url.endsWith('/api/v1/meta')) {
      return Response.json({ name: 'Project Udaan API', environment: 'test', version: 'abcdef1234' })
    }
    return new Response(null, { status: 404 })
  })
}

describe('HomePage', () => {
  it('shows categories and the API build from the backend', async () => {
    mockApi()
    renderAt('/')

    expect(await screen.findByText('Drainage and water')).toBeInTheDocument()
    expect(screen.getByText('शिक्षा')).toBeInTheDocument()
    expect(await screen.findByText(/build abcdef1/)).toBeInTheDocument()
  })

  it('shows the non-production banner and the emergency notice', async () => {
    mockApi()
    renderAt('/')

    expect(screen.getByRole('status')).toHaveTextContent(/environment/i)
    expect(screen.getByText(/In an emergency, call 112/)).toBeInTheDocument()
  })

  it('shows an error when the API is down', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 503 }))
    renderAt('/')

    expect(await screen.findByText('Could not load categories.')).toBeInTheDocument()
  })
})
