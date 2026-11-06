import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { useSession } from '../lib/session'
import { authResponse, defaultHandlers, mockApi, problem, renderAt } from '../test/utils'

const summary = (n: number, extra: object = {}) => ({
  id: `p${n}`,
  publicCode: `UDN-LKO-00000${n}`,
  title: `Problem number ${n}`,
  categoryName: 'Drainage and water',
  localityName: 'Aliganj',
  cityName: 'Lucknow',
  urgency: 'HIGH',
  status: 'OPEN_FOR_SOLUTIONS',
  supporters: n,
  solutions: 0,
  thumbnailUrl: null,
  createdAt: '2026-11-07T10:00:00Z',
  supportedByMe: false,
  ...extra,
})

const lookups = {
  'GET /api/v1/categories': () => Response.json([{ id: 'cat-waste', code: 'WASTE_MANAGEMENT', nameEn: 'Waste management', nameHi: 'कचरा प्रबंधन' }]),
  'GET /api/v1/geo/cities': () => Response.json([{ id: 1, code: 'LKO', name: 'Lucknow', stateId: 26, stateName: 'Uttar Pradesh' }]),
  'GET /api/v1/geo/cities/1/localities': () => Response.json([{ id: 7, name: 'Aliganj' }]),
}

describe('Explore problems', () => {
  it('lists problems for signed-out visitors and loads more on request', async () => {
    const calls = mockApi({
      ...defaultHandlers,
      ...lookups,
      'GET /api/v1/problems': (_b, init) => {
        const page = calls.at(-1)?.query.get('page')
        return Response.json(
          page === '0'
            ? { items: [summary(1), summary(2)], page: 0, size: 20, hasMore: true }
            : { items: [summary(3)], page: 1, size: 20, hasMore: false },
          { headers: init.headers as HeadersInit },
        )
      },
    })
    const user = userEvent.setup()
    renderAt('/problems')

    expect(await screen.findByText('Problem number 1')).toBeInTheDocument()
    expect(screen.getByText('Problem number 2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Show more' }))
    expect(await screen.findByText('Problem number 3')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Show more' })).not.toBeInTheDocument()
    expect(calls.find((c) => c.key === 'GET /api/v1/problems')?.headers.Authorization).toBeUndefined()
  })

  it('puts search and filters in the request and the address bar', async () => {
    const calls = mockApi({ ...defaultHandlers, ...lookups })
    const user = userEvent.setup()
    const { router } = renderAt('/problems')

    await user.type(screen.getByLabelText('Search problems'), 'garbage')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    await user.selectOptions(await screen.findByLabelText('Category'), 'cat-waste')
    await user.selectOptions(screen.getByLabelText('Locality'), await screen.findByRole('option', { name: 'Aliganj' }))

    await waitFor(() => {
      const last = calls.filter((c) => c.key === 'GET /api/v1/problems').at(-1)
      expect(last?.query.get('q')).toBe('garbage')
      expect(last?.query.get('categoryId')).toBe('cat-waste')
      expect(last?.query.get('localityId')).toBe('7')
    })
    expect(router.state.location.search).toContain('q=garbage')
    expect(await screen.findByText('No problems match these filters yet.')).toBeInTheDocument()
    expect(screen.getByLabelText('Sort by')).toHaveValue('RELEVANCE')

    await user.click(screen.getByRole('button', { name: /Clear filters/ }))
    await waitFor(() => expect(router.state.location.search).toBe(''))
  })
})

const detail = {
  id: 'p1',
  publicCode: 'UDN-LKO-000001',
  title: 'Garbage not collected for weeks',
  description: 'Bins overflow and stray animals scatter waste across the lane.',
  category: { id: 'cat-waste', code: 'WASTE_MANAGEMENT', nameEn: 'Waste management', nameHi: 'कचरा प्रबंधन' },
  place: { stateName: 'Uttar Pradesh', cityName: 'Lucknow', localityName: 'Aliganj', addressText: null, latitude: null, longitude: null },
  urgency: 'HIGH',
  peopleAffected: null,
  estimatedImpact: null,
  desiredOutcome: 'Daily collection',
  assistanceTypes: ['VOLUNTEERING'],
  status: 'OPEN_FOR_SOLUTIONS',
  isPublic: true,
  submittedBy: { id: 'u9', displayName: 'Ravi', anonymous: false },
  photos: [],
  counts: { supporters: 4, solutions: 0, comments: 0, followers: 1 },
  timeline: [],
  viewerIsOwner: false,
  viewerSupports: false,
  viewerFollows: false,
  permissions: { canPostUpdates: false, allowedTransitions: [] },
  resolution: null,
  createdAt: '2026-11-07T10:00:00Z',
}

describe('Supporting and following', () => {
  it('supports a problem and updates the count', async () => {
    useSession.setState({ user: authResponse('Asha').user, accessToken: 'access-1', refreshToken: 'r', restoring: false })
    const calls = mockApi({
      ...defaultHandlers,
      'GET /api/v1/problems/UDN-LKO-000001': () => Response.json(detail),
      'PUT /api/v1/problems/UDN-LKO-000001/support': () => Response.json({ active: true, count: 5 }),
      'PUT /api/v1/problems/UDN-LKO-000001/follow': () => Response.json({ active: true, count: 2 }),
    })
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    await user.click(await screen.findByRole('button', { name: 'Support this problem · 4' }))
    expect(await screen.findByRole('button', { name: '✓ Supported · 5' })).toHaveAttribute('aria-pressed', 'true')
    await user.click(screen.getByRole('button', { name: 'Follow' }))
    expect(await screen.findByRole('button', { name: '✓ Following' })).toBeInTheDocument()
    expect(calls.find((c) => c.key === 'PUT /api/v1/problems/UDN-LKO-000001/support')?.headers.Authorization).toBe('Bearer access-1')
  })

  it('asks signed-out visitors to sign in before supporting', async () => {
    mockApi({ ...defaultHandlers, 'GET /api/v1/problems/UDN-LKO-000001': () => Response.json(detail) })
    const user = userEvent.setup()
    const { router } = renderAt('/problems/UDN-LKO-000001')

    await user.click(await screen.findByRole('button', { name: 'Support this problem · 4' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.search).toBe('?next=%2Fproblems%2FUDN-LKO-000001')
  })

  it('shows owners their support count instead of a button', async () => {
    useSession.setState({ user: authResponse('Ravi').user, accessToken: 'a', refreshToken: 'r', restoring: false })
    mockApi({ ...defaultHandlers, 'GET /api/v1/problems/UDN-LKO-000001': () => Response.json({ ...detail, viewerIsOwner: true }) })
    renderAt('/problems/UDN-LKO-000001')

    expect(await screen.findByText('4 people support this problem')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Support this problem/ })).not.toBeInTheDocument()
  })

  it('shows the API error if supporting fails', async () => {
    useSession.setState({ user: authResponse('Asha').user, accessToken: 'a', refreshToken: 'r', restoring: false })
    mockApi({
      ...defaultHandlers,
      'GET /api/v1/problems/UDN-LKO-000001': () => Response.json(detail),
      'PUT /api/v1/problems/UDN-LKO-000001/support': () => problem(404, 'PROBLEM_NOT_FOUND', 'This problem is no longer public'),
    })
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    await user.click(await screen.findByRole('button', { name: 'Support this problem · 4' }))
    expect(await screen.findByText('This problem is no longer public')).toBeInTheDocument()
  })
})

describe('Home page sections', () => {
  it('shows trending problems and links categories to the feed', async () => {
    mockApi({
      ...defaultHandlers,
      ...lookups,
      'GET /api/v1/problems': () => Response.json({ items: [summary(1)], page: 0, size: 4, hasMore: false }),
    })
    renderAt('/')

    const trending = await screen.findByRole('region', { name: 'Trending problems' })
    await waitFor(() => expect(trending).toHaveTextContent('Problem number 1'))
    expect(await screen.findByRole('link', { name: /Waste management/ })).toHaveAttribute('href', '/problems?categoryId=cat-waste')
  })
})
