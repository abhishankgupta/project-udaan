import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { useSession } from '../lib/session'
import { authResponse, defaultHandlers, mockApi, renderAt } from '../test/utils'

vi.mock('../features/problems/compressImage', () => ({ compressImage: async (f: File) => f }))

const signedInAs = (name: string) =>
  useSession.setState({ user: authResponse(name).user, accessToken: 'access-1', refreshToken: 'r', restoring: false })

const problem = (extra: object = {}) => ({
  id: 'p1',
  publicCode: 'UDN-LKO-000001',
  title: 'Streetlight out at the bus stop',
  description: 'The only streetlight at the bus stop has been dark for a month.',
  category: { id: 'c', code: 'ROADS_INFRASTRUCTURE', nameEn: 'Roads and infrastructure', nameHi: 'सड़क' },
  place: { stateName: 'Uttar Pradesh', cityName: 'Lucknow', localityName: 'Kapoorthala', addressText: null, latitude: null, longitude: null },
  urgency: 'MEDIUM',
  peopleAffected: null,
  estimatedImpact: null,
  desiredOutcome: 'A working light',
  assistanceTypes: ['GOVERNMENT_SUPPORT'],
  status: 'SOLUTION_ACCEPTED',
  isPublic: true,
  submittedBy: { id: 'u1', displayName: 'Owner', anonymous: false },
  photos: [],
  counts: { supporters: 2, solutions: 1, comments: 0, followers: 3 },
  timeline: [
    {
      type: 'PROGRESS', fromStatus: null, toStatus: null, body: 'New pole arrives Friday.', authorName: 'Proposer',
      authorRole: 'ACCEPTED_PROPOSER', photos: [{ id: 'm5', url: '/api/v1/media/m5', width: 10, height: 10 }], at: '2026-11-07T10:00:00Z',
    },
  ],
  viewerIsOwner: true,
  viewerSupports: false,
  viewerFollows: false,
  permissions: { canPostUpdates: true, allowedTransitions: ['IN_PROGRESS', 'RESOLVED', 'CLOSED'] },
  resolution: null,
  createdAt: '2026-11-07T09:00:00Z',
  ...extra,
})

const handlers = (extra: Record<string, Parameters<typeof mockApi>[0][string]> = {}) => ({
  ...defaultHandlers,
  'GET /api/v1/problems/UDN-LKO-000001': () => Response.json(problem()),
  'GET /api/v1/problems/UDN-LKO-000001/solutions': () => Response.json([]),
  'GET /api/v1/problems/UDN-LKO-000001/contributions': () => Response.json([]),
  'GET /api/v1/problems/UDN-LKO-000001/comments': () => Response.json([]),
  'GET /api/v1/notifications/unread-count': () => Response.json({ unreadCount: 0 }),
  ...extra,
})

describe('Managing a problem', () => {
  it('lets the owner start work', async () => {
    signedInAs('Owner')
    const calls = mockApi(
      handlers({ 'POST /api/v1/problems/UDN-LKO-000001/status-transitions': () => Response.json(problem({ status: 'IN_PROGRESS' })) }),
    )
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    await user.click(await screen.findByRole('button', { name: 'Mark work as started' }))
    await waitFor(() =>
      expect(calls.find((c) => c.key === 'POST /api/v1/problems/UDN-LKO-000001/status-transitions')?.body).toEqual({ toStatus: 'IN_PROGRESS' }),
    )
    expect((await screen.findAllByText('In progress')).length).toBeGreaterThan(0)
  })

  it('requires a note and a photo to mark a problem resolved', async () => {
    signedInAs('Owner')
    const calls = mockApi(
      handlers({
        'POST /api/v1/media': () => Response.json({ id: 'm9', url: '/api/v1/media/m9', width: 10, height: 10 }, { status: 201 }),
        'POST /api/v1/problems/UDN-LKO-000001/status-transitions': () => Response.json(problem({ status: 'RESOLVED' })),
      }),
    )
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    await user.click(await screen.findByRole('button', { name: 'Mark as resolved' }))
    await user.type(screen.getByLabelText('What was done to resolve it?'), 'New light installed on Friday')
    await user.click(screen.getAllByRole('button', { name: 'Mark as resolved' }).at(-1)!)
    expect(screen.getByRole('alert')).toHaveTextContent('Add at least one photo')
    expect(calls.some((c) => c.key.includes('status-transitions'))).toBe(false)

    await user.upload(screen.getByLabelText('Add photos'), new File(['x'], 'fixed.jpg', { type: 'image/jpeg' }))
    expect(await screen.findByAltText('Photo 1')).toBeInTheDocument()
    await user.click(screen.getAllByRole('button', { name: 'Mark as resolved' }).at(-1)!)

    await waitFor(() =>
      expect(calls.find((c) => c.key === 'POST /api/v1/problems/UDN-LKO-000001/status-transitions')?.body).toEqual({
        toStatus: 'RESOLVED',
        note: 'New light installed on Friday',
        evidenceMediaIds: ['m9'],
      }),
    )
  })

  it('lets the solution proposer post a progress update', async () => {
    signedInAs('Proposer')
    const calls = mockApi(
      handlers({
        'GET /api/v1/problems/UDN-LKO-000001': () =>
          Response.json(problem({ viewerIsOwner: false, permissions: { canPostUpdates: true, allowedTransitions: ['IN_PROGRESS'] } })),
        'POST /api/v1/problems/UDN-LKO-000001/updates': () => Response.json(problem({ viewerIsOwner: false }), { status: 201 }),
      }),
    )
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    expect(await screen.findByText("You're delivering the accepted solution")).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mark as resolved' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Post a progress update' }))
    await user.type(screen.getByLabelText('Progress update'), 'Ward office approved the work.')
    await user.click(screen.getByRole('button', { name: 'Post update' }))

    await waitFor(() =>
      expect(calls.find((c) => c.key === 'POST /api/v1/problems/UDN-LKO-000001/updates')?.body).toEqual({
        body: 'Ward office approved the work.',
        mediaIds: [],
      }),
    )
  })

  it('shows progress updates with their author and photos, and hides the panel from others', async () => {
    signedInAs('Visitor')
    mockApi(
      handlers({
        'GET /api/v1/problems/UDN-LKO-000001': () =>
          Response.json(problem({ viewerIsOwner: false, permissions: { canPostUpdates: false, allowedTransitions: [] } })),
      }),
    )
    renderAt('/problems/UDN-LKO-000001')

    expect(await screen.findByText('New pole arrives Friday.')).toBeInTheDocument()
    expect(screen.getByText(/Proposer \(delivering the solution\)/)).toBeInTheDocument()
    expect(screen.getByAltText('Update photo 1')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: /Manage your problem/ })).not.toBeInTheDocument()
  })
})

describe('Confirming a resolution', () => {
  const resolved = (extra: object = {}) =>
    problem({
      status: 'RESOLVED',
      viewerIsOwner: false,
      permissions: { canPostUpdates: false, allowedTransitions: [] },
      resolution: { confirms: 1, disputes: 0, viewerVote: null, votingOpen: true, votingClosesAt: '2026-11-21T10:00:00Z' },
      ...extra,
    })

  it('lets neighbours confirm the fix', async () => {
    signedInAs('Neighbour')
    const calls = mockApi(
      handlers({
        'GET /api/v1/problems/UDN-LKO-000001': () => Response.json(resolved()),
        'POST /api/v1/problems/UDN-LKO-000001/resolution-votes': () =>
          Response.json(resolved({ resolution: { confirms: 2, disputes: 0, viewerVote: 'CONFIRM', votingOpen: true, votingClosesAt: '2026-11-21T10:00:00Z' } })),
      }),
    )
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    await user.click(await screen.findByRole('button', { name: "Yes, it's fixed" }))
    expect(await screen.findByText(/2 people confirm/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: "Yes, it's fixed" })).toHaveAttribute('aria-pressed', 'true')
    expect(calls.find((c) => c.key === 'POST /api/v1/problems/UDN-LKO-000001/resolution-votes')?.body).toEqual({ vote: 'CONFIRM' })
  })

  it('does not ask the owner to vote', async () => {
    signedInAs('Owner')
    mockApi(handlers({ 'GET /api/v1/problems/UDN-LKO-000001': () => Response.json(resolved({ viewerIsOwner: true })) }))
    renderAt('/problems/UDN-LKO-000001')

    expect(await screen.findByText('Marked as resolved')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: "Yes, it's fixed" })).not.toBeInTheDocument()
  })
})

describe('Notifications', () => {
  const inbox = {
    items: [
      { id: 'n1', type: 'SOLUTION_PROPOSED', title: 'Proposer proposed a solution to “Streetlight out”', body: 'Ask the ward office', link: '/problems/UDN-LKO-000001', read: false, createdAt: '2026-11-07T10:00:00Z' },
      { id: 'n2', type: 'COMMENT', title: 'Alice commented', body: null, link: '/problems/UDN-LKO-000001', read: true, createdAt: '2026-11-07T09:00:00Z' },
    ],
    unreadCount: 1,
  }

  it('shows the unread count on the bell', async () => {
    signedInAs('Owner')
    mockApi({ ...defaultHandlers, 'GET /api/v1/notifications/unread-count': () => Response.json({ unreadCount: 3 }) })
    renderAt('/')

    expect(await screen.findByRole('link', { name: 'Notifications, 3 unread' })).toHaveTextContent('3')
  })

  it('lists notifications, marks one read when opened, and marks all read', async () => {
    signedInAs('Owner')
    const calls = mockApi(
      handlers({
        'GET /api/v1/notifications': () => Response.json(inbox),
        'POST /api/v1/notifications/read': () => Response.json({ unreadCount: 0 }),
      }),
    )
    const user = userEvent.setup()
    const { router } = renderAt('/notifications')

    expect(await screen.findByText(/Proposer proposed a solution/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Mark all as read' }))
    await waitFor(() => expect(calls.filter((c) => c.key === 'POST /api/v1/notifications/read')[0]?.body).toEqual({}))

    await user.click(screen.getByText(/Proposer proposed a solution/))
    await waitFor(() => expect(router.state.location.pathname).toBe('/problems/UDN-LKO-000001'))
    expect(calls.filter((c) => c.key === 'POST /api/v1/notifications/read').at(-1)?.body).toEqual({ ids: ['n1'] })
  })
})
