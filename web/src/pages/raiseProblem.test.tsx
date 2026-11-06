import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { useSession } from '../lib/session'
import { authResponse, defaultHandlers, mockApi, problem, renderAt } from '../test/utils'

vi.mock('../features/problems/compressImage', () => ({ compressImage: async (f: File) => f }))

const signedIn = () =>
  useSession.setState({ user: authResponse('Asha').user, accessToken: 'access-1', refreshToken: 'r', restoring: false })

const handlers = {
  ...defaultHandlers,
  'GET /api/v1/categories': () =>
    Response.json([{ id: 'cat-drain', code: 'DRAINAGE_WATER', nameEn: 'Drainage and water', nameHi: 'जल निकासी और पानी' }]),
  'GET /api/v1/geo/cities': () => Response.json([{ id: 1, code: 'LKO', name: 'Lucknow', stateId: 26, stateName: 'Uttar Pradesh' }]),
  'GET /api/v1/geo/cities/1/localities': () => Response.json([{ id: 7, name: 'Aliganj' }, { id: 9, name: 'Hazratganj' }]),
}

const created = {
  id: 'p1',
  publicCode: 'UDN-LKO-000042',
  title: 'Waterlogging on Station Road after rain',
  description: 'The road floods knee-deep every monsoon and nobody can pass.',
  category: { id: 'cat-drain', code: 'DRAINAGE_WATER', nameEn: 'Drainage and water', nameHi: 'जल निकासी और पानी' },
  place: { stateName: 'Uttar Pradesh', cityName: 'Lucknow', localityName: 'Aliganj', addressText: null, latitude: null, longitude: null },
  urgency: 'HIGH',
  peopleAffected: 2000,
  estimatedImpact: null,
  desiredOutcome: 'Working drains before next monsoon',
  assistanceTypes: ['GOVERNMENT_SUPPORT'],
  status: 'UNDER_REVIEW',
  isPublic: false,
  submittedBy: { id: 'u1', displayName: 'Asha', anonymous: false },
  photos: [{ id: 'm1', url: '/api/v1/media/m1', width: 1600, height: 1200 }],
  counts: { supporters: 0, solutions: 0, comments: 0, followers: 0 },
  timeline: [{ type: 'STATUS_CHANGE', fromStatus: 'SUBMITTED', toStatus: 'UNDER_REVIEW', body: 'Submitted for review', authorName: 'Asha', authorRole: 'OWNER', photos: [], at: '2026-11-07T10:00:00Z' }],
  viewerIsOwner: true,
  viewerSupports: false,
  viewerFollows: false,
  permissions: { canPostUpdates: false, allowedTransitions: [] },
  resolution: null,
  createdAt: '2026-11-07T10:00:00Z',
}

async function fillRequiredFields(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Title'), 'Waterlogging on Station Road after rain')
  await user.selectOptions(await screen.findByLabelText('Category'), 'cat-drain')
  await user.type(screen.getByLabelText('Description'), 'The road floods knee-deep every monsoon and nobody can pass.')
  await waitFor(() => expect(screen.getByLabelText('City')).toHaveValue('1'))
  await user.selectOptions(await screen.findByLabelText('Locality'), await screen.findByRole('option', { name: 'Aliganj' }))
  await user.click(screen.getByLabelText(/High — needs attention soon/))
  await user.click(screen.getByLabelText('Government action or approval'))
  await user.type(screen.getByLabelText('What outcome do you want?'), 'Working drains before next monsoon')
}

describe('Raising a problem', () => {
  it('uploads a photo, submits the problem and shows the confirmation', async () => {
    signedIn()
    const calls = mockApi({
      ...handlers,
      'POST /api/v1/media': () => Response.json({ id: 'm1', url: '/api/v1/media/m1', width: 1600, height: 1200 }, { status: 201 }),
      'POST /api/v1/problems': () => Response.json(created, { status: 201 }),
      'GET /api/v1/problems/UDN-LKO-000042': () => Response.json(created),
    })
    const user = userEvent.setup()
    const { router } = renderAt('/problems/new')

    await fillRequiredFields(user)
    await user.type(screen.getByLabelText('People affected (optional)'), '2000')
    const file = new File(['fake-image'], 'road.jpg', { type: 'image/jpeg' })
    await user.upload(screen.getByLabelText('Add photos'), file)
    expect(await screen.findByAltText('Photo 1')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Submit problem' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/problems/UDN-LKO-000042'))
    expect(await screen.findByText(/Moderators will review it shortly/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: created.title })).toBeInTheDocument()

    const upload = calls.find((c) => c.key === 'POST /api/v1/media')
    expect(upload?.headers.Authorization).toBe('Bearer access-1')
    expect(calls.find((c) => c.key === 'POST /api/v1/problems')?.body).toMatchObject({
      title: 'Waterlogging on Station Road after rain',
      categoryId: 'cat-drain',
      cityId: 1,
      localityId: 7,
      localityText: null,
      urgency: 'HIGH',
      peopleAffected: 2000,
      assistanceTypes: ['GOVERNMENT_SUPPORT'],
      anonymous: false,
      mediaIds: ['m1'],
    })
    expect(localStorage.getItem('udaan.problemDraft')).toBeNull()
  })

  it('points out missing fields without calling the API', async () => {
    signedIn()
    const calls = mockApi(handlers)
    const user = userEvent.setup()
    renderAt('/problems/new')

    await user.click(await screen.findByRole('button', { name: 'Submit problem' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Please fix the highlighted fields.')
    expect(screen.getByText('Give a title of at least 10 characters.')).toBeInTheDocument()
    expect(screen.getByText('Choose at least one kind of help.')).toBeInTheDocument()
    expect(calls.some((c) => c.key === 'POST /api/v1/problems')).toBe(false)
  })

  it('lets people type a locality that is not listed', async () => {
    signedIn()
    const calls = mockApi({ ...handlers, 'POST /api/v1/problems': () => Response.json(created, { status: 201 }) })
    const user = userEvent.setup()
    renderAt('/problems/new')

    await fillRequiredFields(user)
    await user.selectOptions(screen.getByLabelText('Locality'), 'Other (type it)')
    await user.type(screen.getByLabelText('Locality name'), 'New Colony')
    await user.click(screen.getByRole('button', { name: 'Submit problem' }))

    await waitFor(() => expect(calls.some((c) => c.key === 'POST /api/v1/problems')).toBe(true))
    expect(calls.find((c) => c.key === 'POST /api/v1/problems')?.body).toMatchObject({ localityId: null, localityText: 'New Colony' })
  })

  it('shows emergency numbers for critical problems', async () => {
    signedIn()
    mockApi(handlers)
    const user = userEvent.setup()
    renderAt('/problems/new')

    await user.click(await screen.findByLabelText(/Critical — risk to health or safety/))
    expect(screen.getByRole('note')).toHaveTextContent('112')
  })

  it('restores an unsent draft', async () => {
    signedIn()
    localStorage.setItem('udaan.problemDraft', JSON.stringify({ title: 'Broken streetlights on Kursi Road' }))
    mockApi(handlers)
    renderAt('/problems/new')

    expect(await screen.findByLabelText('Title')).toHaveValue('Broken streetlights on Kursi Road')
  })

  it('shows API errors for a field', async () => {
    signedIn()
    mockApi({
      ...handlers,
      'POST /api/v1/problems': () =>
        problem(400, 'VALIDATION_FAILED', 'Choose your locality or type its name', {
          fieldErrors: [{ field: 'localityId', message: 'Choose your locality or type its name' }],
        }),
    })
    const user = userEvent.setup()
    renderAt('/problems/new')

    await fillRequiredFields(user)
    await user.click(screen.getByRole('button', { name: 'Submit problem' }))

    expect(await screen.findByText('Choose your locality or type its name')).toBeInTheDocument()
  })
})

describe('Problem pages', () => {
  it('shows a friendly message for problems that are not public', async () => {
    mockApi({ ...defaultHandlers, 'GET /api/v1/problems/UDN-LKO-000099': () => problem(404, 'PROBLEM_NOT_FOUND', 'Not found') })
    renderAt('/problems/UDN-LKO-000099')

    expect(await screen.findByRole('heading', { name: 'Problem not found' })).toBeInTheDocument()
  })

  it('shows public problems with their timeline and hides anonymous submitters', async () => {
    mockApi({
      ...defaultHandlers,
      'GET /api/v1/problems/UDN-LKO-000042': () =>
        Response.json({
          ...created,
          status: 'OPEN_FOR_SOLUTIONS',
          isPublic: true,
          viewerIsOwner: false,
          submittedBy: { id: null, displayName: null, anonymous: true },
        }),
    })
    renderAt('/problems/UDN-LKO-000042')

    expect(await screen.findByText(/an anonymous resident/)).toBeInTheDocument()
    expect(screen.getAllByText('Open for solutions').length).toBeGreaterThan(0)
    expect(screen.getByText('Submitted for review')).toBeInTheDocument()
  })

  it('lists the signed-in user\'s problems', async () => {
    signedIn()
    mockApi({
      ...defaultHandlers,
      'GET /api/v1/me/problems': () =>
        Response.json([
          {
            id: 'p1', publicCode: 'UDN-LKO-000042', title: 'Waterlogging on Station Road', categoryName: 'Drainage and water',
            localityName: 'Aliganj', cityName: 'Lucknow', urgency: 'HIGH', status: 'UNDER_REVIEW', supporters: 0, solutions: 0,
            thumbnailUrl: null, createdAt: '2026-11-07T10:00:00Z',
          },
        ]),
    })
    renderAt('/me/problems')

    expect(await screen.findByText('Waterlogging on Station Road')).toBeInTheDocument()
    expect(screen.getByText('Under review')).toBeInTheDocument()
  })
})

describe('Duplicate hints', () => {
  it('suggests similar problems while the title is typed', async () => {
    useSession.setState({ user: authResponse('Asha').user, accessToken: 'a', refreshToken: 'r', restoring: false })
    mockApi({
      ...handlers,
      'GET /api/v1/problems/similar': () =>
        Response.json([
          {
            id: 'p5', publicCode: 'UDN-LKO-000005', title: 'Waterlogging on Station Road', categoryName: 'Drainage and water',
            localityName: 'Aliganj', cityName: 'Lucknow', urgency: 'HIGH', status: 'OPEN_FOR_SOLUTIONS', supporters: 12, solutions: 1,
            thumbnailUrl: null, createdAt: '2026-11-01T10:00:00Z',
          },
        ]),
    })
    const user = userEvent.setup()
    renderAt('/problems/new')

    await user.type(await screen.findByLabelText('Title'), 'Waterlogging near Station Road')
    expect(await screen.findByText(/may already have been reported/, {}, { timeout: 3000 })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Waterlogging on Station Road' })).toHaveAttribute('href', '/problems/UDN-LKO-000005')
  })
})
