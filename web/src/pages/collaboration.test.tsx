import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { useSession } from '../lib/session'
import { authResponse, defaultHandlers, mockApi, renderAt } from '../test/utils'

const signedInAs = (name: string) =>
  useSession.setState({ user: authResponse(name).user, accessToken: 'access-1', refreshToken: 'r', restoring: false })

const problemView = (extra: object = {}) => ({
  id: 'p1',
  publicCode: 'UDN-LKO-000001',
  title: 'Broken drain cover near the market',
  description: 'An open drain near the market is a hazard for children.',
  category: { id: 'c', code: 'DRAINAGE_WATER', nameEn: 'Drainage and water', nameHi: 'जल' },
  place: { stateName: 'Uttar Pradesh', cityName: 'Lucknow', localityName: 'Chowk', addressText: null, latitude: null, longitude: null },
  urgency: 'HIGH',
  peopleAffected: null,
  estimatedImpact: null,
  desiredOutcome: 'A strong cover installed',
  assistanceTypes: ['FUNDING'],
  status: 'SOLUTION_PROPOSED',
  isPublic: true,
  submittedBy: { id: 'u1', displayName: 'Owner', anonymous: false },
  photos: [],
  counts: { supporters: 0, solutions: 1, comments: 0, followers: 0 },
  timeline: [],
  viewerIsOwner: false,
  viewerSupports: false,
  viewerFollows: false,
  permissions: { canPostUpdates: false, allowedTransitions: [] },
  resolution: null,
  createdAt: '2026-11-07T10:00:00Z',
  ...extra,
})

const solution = (extra: object = {}) => ({
  id: 's1',
  problem: { id: 'p1', publicCode: 'UDN-LKO-000001', title: 'Broken drain cover near the market', status: 'SOLUTION_PROPOSED' },
  title: 'Cast-iron cover funded by shops',
  summary: 'Install a cast-iron cover and a warning sign.',
  approach: 'Collect quotes, pool money, install within a week.',
  estimatedCostInr: 15000,
  timelineText: '2 weeks',
  resourcesRequired: null,
  skills: ['Civil work'],
  status: 'PUBLISHED',
  proposer: { id: 'u2', displayName: 'Proposer' },
  endorsements: 3,
  viewerEndorsed: false,
  viewerIsProposer: false,
  viewerIsProblemOwner: false,
  canAccept: false,
  createdAt: '2026-11-07T11:00:00Z',
  ...extra,
})

const baseHandlers = (extra: Record<string, Parameters<typeof mockApi>[0][string]> = {}) => ({
  ...defaultHandlers,
  'GET /api/v1/problems/UDN-LKO-000001': () => Response.json(problemView()),
  'GET /api/v1/problems/UDN-LKO-000001/solutions': () => Response.json([solution()]),
  'GET /api/v1/problems/UDN-LKO-000001/contributions': () => Response.json([]),
  'GET /api/v1/problems/UDN-LKO-000001/comments': () => Response.json([]),
  ...extra,
})

describe('Solutions on a problem', () => {
  it('lets the owner accept a solution', async () => {
    signedInAs('Owner')
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const calls = mockApi(
      baseHandlers({
        'GET /api/v1/problems/UDN-LKO-000001': () => Response.json(problemView({ viewerIsOwner: true })),
        'GET /api/v1/problems/UDN-LKO-000001/solutions': () => Response.json([solution({ canAccept: true, viewerIsProblemOwner: true })]),
        'POST /api/v1/solutions/s1/accept': () => Response.json(solution({ status: 'ACCEPTED' })),
      }),
    )
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    expect(await screen.findByText('Cast-iron cover funded by shops')).toBeInTheDocument()
    expect(screen.getByText(/Est. cost ₹15,000/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Accept this solution' }))
    await waitFor(() => expect(calls.some((c) => c.key === 'POST /api/v1/solutions/s1/accept')).toBe(true))
  })

  it('endorses a solution', async () => {
    signedInAs('Asha')
    const calls = mockApi(baseHandlers({ 'PUT /api/v1/solutions/s1/endorsement': () => Response.json({ active: true, count: 4 }) }))
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    await user.click(await screen.findByRole('button', { name: 'Endorse' }))
    await waitFor(() => expect(calls.some((c) => c.key === 'PUT /api/v1/solutions/s1/endorsement')).toBe(true))
  })

  it('proposes a solution and opens it', async () => {
    signedInAs('Asha')
    const calls = mockApi(
      baseHandlers({
        'POST /api/v1/problems/UDN-LKO-000001/solutions': () => Response.json(solution({ id: 's9' }), { status: 201 }),
        'GET /api/v1/solutions/s9': () => Response.json(solution({ id: 's9', viewerIsProposer: true })),
        'GET /api/v1/solutions/s9/comments': () => Response.json([]),
      }),
    )
    const user = userEvent.setup()
    const { router } = renderAt('/problems/UDN-LKO-000001/solutions/new')

    await user.type(screen.getByLabelText('Solution title'), 'Cast-iron cover funded by shops')
    await user.type(screen.getByLabelText('Summary'), 'Install a cast-iron cover and a warning sign.')
    await user.type(screen.getByLabelText('How would it be done?'), 'Collect quotes, pool money, install within a week.')
    await user.type(screen.getByLabelText('Estimated cost in ₹ (optional)'), '15000')
    await user.type(screen.getByLabelText('Skills needed (optional)'), 'Civil work, Fundraising')
    await user.click(screen.getByRole('button', { name: 'Publish solution' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/solutions/s9'))
    expect(calls.find((c) => c.key === 'POST /api/v1/problems/UDN-LKO-000001/solutions')?.body).toMatchObject({
      title: 'Cast-iron cover funded by shops',
      estimatedCostInr: 15000,
      skills: ['Civil work', 'Fundraising'],
    })
    expect(await screen.findByRole('button', { name: 'Withdraw' })).toBeInTheDocument()
  })

  it('checks the solution form before sending', async () => {
    signedInAs('Asha')
    const calls = mockApi(baseHandlers())
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001/solutions/new')

    await user.click(screen.getByRole('button', { name: 'Publish solution' }))
    expect(screen.getByRole('alert')).toHaveTextContent('Please fix the highlighted fields.')
    expect(calls.some((c) => c.key.startsWith('POST'))).toBe(false)
  })
})

describe('Discussion', () => {
  it('posts a comment and a reply', async () => {
    signedInAs('Asha')
    let comments: object[] = []
    const calls = mockApi(
      baseHandlers({
        'GET /api/v1/problems/UDN-LKO-000001/comments': () => Response.json(comments),
        'POST /api/v1/problems/UDN-LKO-000001/comments': (body) => {
          const b = body as { body: string; parentId?: string }
          const c = { id: `c${comments.length + 1}`, authorId: 'u1', authorName: 'Asha', body: b.body, removed: false, viewerIsAuthor: true, createdAt: '2026-11-07T12:00:00Z', replies: [] }
          comments = b.parentId ? comments.map((x) => ({ ...x, replies: [c] })) : [...comments, c]
          return Response.json(c, { status: 201 })
        },
      }),
    )
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    await user.type(await screen.findByPlaceholderText(/Share information/), 'I can measure the drain.')
    await user.click(screen.getByRole('button', { name: 'Post comment' }))
    expect(await screen.findByText('I can measure the drain.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Reply' }))
    await user.type(screen.getByPlaceholderText('Write a reply…'), 'Saturday?')
    await user.click(screen.getByRole('button', { name: 'Post reply' }))
    expect(await within(await screen.findByRole('list', { name: 'Replies' })).findByText('Saturday?')).toBeInTheDocument()
    expect(calls.filter((c) => c.key === 'POST /api/v1/problems/UDN-LKO-000001/comments').at(-1)?.body).toEqual({ body: 'Saturday?', parentId: 'c1' })
  })

  it('invites signed-out visitors to sign in', async () => {
    mockApi(baseHandlers())
    renderAt('/problems/UDN-LKO-000001')
    expect(await screen.findByText(/to join the discussion/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Post comment' })).not.toBeInTheDocument()
  })
})

describe('Offers of help', () => {
  it('sends an offer with the chosen kinds of help', async () => {
    signedInAs('Helper')
    const calls = mockApi(
      baseHandlers({ 'POST /api/v1/problems/UDN-LKO-000001/contributions': () => Response.json({}, { status: 201 }) }),
    )
    const user = userEvent.setup()
    renderAt('/problems/UDN-LKO-000001')

    await user.click(await screen.findByRole('button', { name: 'I want to help' }))
    await user.click(screen.getByLabelText('Volunteers'))
    await user.click(screen.getByLabelText('Funding'))
    await user.selectOptions(screen.getByLabelText(/Towards a specific solution/), 's1')
    await user.type(screen.getByLabelText(/Message/), 'Free on Sundays.')
    await user.click(screen.getByLabelText(/Share my email\/phone/))
    await user.click(screen.getByRole('button', { name: 'Send offer' }))

    await waitFor(() =>
      expect(calls.find((c) => c.key === 'POST /api/v1/problems/UDN-LKO-000001/contributions')?.body).toEqual({
        types: ['VOLUNTEERING', 'FUNDING'],
        message: 'Free on Sundays.',
        solutionId: 's1',
        shareContact: true,
      }),
    )
  })

  it('shows the owner the message and shared contact', async () => {
    signedInAs('Owner')
    mockApi(
      baseHandlers({
        'GET /api/v1/problems/UDN-LKO-000001/contributions': () =>
          Response.json([
            {
              id: 'o1', contributorId: 'u3', contributorName: 'Helper', types: ['VOLUNTEERING'], solutionId: null, solutionTitle: null,
              problemCode: 'UDN-LKO-000001', problemTitle: 'Broken drain cover', message: 'Free on Sundays.',
              contact: { email: 'helper@example.org', phone: null }, viewerIsContributor: false, createdAt: '2026-11-07T12:00:00Z',
            },
          ]),
      }),
    )
    renderAt('/problems/UDN-LKO-000001')

    expect(await screen.findByText(/Helper/, { selector: 'span' })).toBeInTheDocument()
    expect(screen.getByText('“Free on Sundays.”')).toBeInTheDocument()
    expect(screen.getByText('Contact: helper@example.org')).toBeInTheDocument()
  })

  it('asks signed-out visitors to sign in before offering', async () => {
    mockApi(baseHandlers())
    const user = userEvent.setup()
    const { router } = renderAt('/problems/UDN-LKO-000001')

    await user.click(await screen.findByRole('button', { name: 'I want to help' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
  })
})

describe('My contributions', () => {
  it('lists problems, solutions and offers', async () => {
    signedInAs('Asha')
    mockApi({
      ...defaultHandlers,
      'GET /api/v1/me/problems': () => Response.json([]),
      'GET /api/v1/me/solutions': () => Response.json([solution({ viewerIsProposer: true })]),
      'GET /api/v1/me/contributions': () =>
        Response.json([
          {
            id: 'o1', contributorId: 'u1', contributorName: 'Asha', types: ['FUNDING'], solutionId: null, solutionTitle: null,
            problemCode: 'UDN-LKO-000001', problemTitle: 'Broken drain cover', message: null, contact: null, viewerIsContributor: true,
            createdAt: '2026-11-07T12:00:00Z',
          },
        ]),
    })
    renderAt('/me/contributions')

    expect(await screen.findByText('Cast-iron cover funded by shops')).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Broken drain cover' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My contributions' })).toBeInTheDocument()
  })
})
