import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../../lib/api'
import type { AssistanceType, ProblemStatus, Urgency } from '../../lib/labels'
import { useSession } from '../../lib/session'

export interface Photo {
  id: string
  url: string
  width: number
  height: number
}

export interface ProblemDraft {
  title: string
  description: string
  categoryId: string
  cityId: number
  localityId: number | null
  localityText: string | null
  addressText: string
  latitude: number | null
  longitude: number | null
  urgency: Urgency
  peopleAffected: number | null
  estimatedImpact: string
  desiredOutcome: string
  assistanceTypes: AssistanceType[]
  anonymous: boolean
  mediaIds: string[]
}

export interface ProblemView {
  id: string
  publicCode: string
  title: string
  description: string
  category: { id: string; code: string; nameEn: string; nameHi: string }
  place: {
    stateName: string | null
    cityName: string | null
    localityName: string | null
    addressText: string | null
    latitude: number | null
    longitude: number | null
  }
  urgency: Urgency
  peopleAffected: number | null
  estimatedImpact: string | null
  desiredOutcome: string
  assistanceTypes: AssistanceType[]
  status: ProblemStatus
  isPublic: boolean
  submittedBy: { id: string | null; displayName: string | null; anonymous: boolean }
  photos: Photo[]
  counts: { supporters: number; solutions: number; comments: number; followers: number }
  timeline: TimelineEntry[]
  viewerIsOwner: boolean
  viewerSupports: boolean
  viewerFollows: boolean
  permissions: { canPostUpdates: boolean; allowedTransitions: ProblemStatus[] }
  resolution: { confirms: number; disputes: number; viewerVote: 'CONFIRM' | 'DISPUTE' | null; votingOpen: boolean; votingClosesAt: string | null } | null
  createdAt: string
}

export interface TimelineEntry {
  type: 'STATUS_CHANGE' | 'PROGRESS' | 'OFFICIAL_RESPONSE' | 'RESOLUTION_EVIDENCE'
  fromStatus: ProblemStatus | null
  toStatus: ProblemStatus | null
  body: string | null
  authorName: string | null
  authorRole: 'OWNER' | 'ACCEPTED_PROPOSER' | 'COMMUNITY' | null
  photos: Photo[]
  at: string
}

export interface ProblemSummary {
  id: string
  publicCode: string
  title: string
  categoryName: string
  localityName: string | null
  cityName: string | null
  urgency: Urgency
  status: ProblemStatus
  supporters: number
  solutions: number
  thumbnailUrl: string | null
  createdAt: string
  supportedByMe?: boolean
}

export type FeedSort = 'RECENT' | 'SUPPORTED' | 'TRENDING' | 'IMPACT' | 'RELEVANCE'

export interface FeedFilters {
  q?: string
  localityId?: string
  categoryId?: string
  status?: string
  urgency?: string
  assistance?: string
  sort?: FeedSort
}

export interface FeedPage {
  items: ProblemSummary[]
  page: number
  size: number
  hasMore: boolean
}

export function uploadPhoto(file: Blob, filename: string) {
  const form = new FormData()
  form.append('file', file, filename)
  return apiRequest<Photo>('/api/v1/media', { method: 'POST', body: form, auth: true })
}

export function useSubmitProblem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (draft: ProblemDraft) => apiRequest<ProblemView>('/api/v1/problems', { body: draft, auth: true }),
    onSuccess: (problem) => {
      queryClient.setQueryData(['problem', problem.publicCode], problem)
      void queryClient.invalidateQueries({ queryKey: ['my-problems'] })
    },
  })
}

export function useProblem(code: string) {
  const signedIn = useSession((s) => s.user !== null)
  const restoring = useSession((s) => s.restoring)
  return useQuery({
    queryKey: ['problem', code],
    // Send the token when signed in, so submitters can see their own problems while under review.
    queryFn: () => apiRequest<ProblemView>(`/api/v1/problems/${encodeURIComponent(code)}`, { auth: signedIn }),
    enabled: !restoring,
  })
}

export function useMyProblems() {
  return useQuery({
    queryKey: ['my-problems'],
    queryFn: () => apiRequest<ProblemSummary[]>('/api/v1/me/problems', { auth: true }),
  })
}

function feedParams(filters: FeedFilters, page: number, size: number) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) if (value) params.set(key, value)
  params.set('page', String(page))
  params.set('size', String(size))
  return params.toString()
}

export function useFeed(filters: FeedFilters, size = 20) {
  const signedIn = useSession((s) => s.user !== null)
  const restoring = useSession((s) => s.restoring)
  return useInfiniteQuery({
    queryKey: ['feed', filters, size, signedIn],
    queryFn: ({ pageParam }) => apiRequest<FeedPage>(`/api/v1/problems?${feedParams(filters, pageParam, size)}`, { auth: signedIn }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasMore ? last.page + 1 : undefined),
    enabled: !restoring,
  })
}

export function useSimilarProblems(title: string, cityId: number | null, localityId: number | null) {
  const trimmed = title.trim()
  return useQuery({
    queryKey: ['similar', trimmed, cityId, localityId],
    queryFn: () => {
      const params = new URLSearchParams({ title: trimmed, cityId: String(cityId) })
      if (localityId) params.set('localityId', String(localityId))
      return apiRequest<ProblemSummary[]>(`/api/v1/problems/similar?${params.toString()}`)
    },
    enabled: trimmed.length >= 10 && cityId !== null,
    staleTime: 60_000,
  })
}

export interface Toggle {
  active: boolean
  count: number
}

/** Support or follow on/off, updating the cached problem so the page reflects it straight away. */
export function useToggle(code: string, kind: 'support' | 'follow') {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (on: boolean) =>
      apiRequest<Toggle>(`/api/v1/problems/${encodeURIComponent(code)}/${kind}`, { method: on ? 'PUT' : 'DELETE', auth: true }),
    onSuccess: (result) => {
      queryClient.setQueryData<ProblemView>(['problem', code], (p) =>
        p && kind === 'support'
          ? { ...p, viewerSupports: result.active, counts: { ...p.counts, supporters: result.count } }
          : p
            ? { ...p, viewerFollows: result.active, counts: { ...p.counts, followers: result.count } }
            : p,
      )
      void queryClient.invalidateQueries({ queryKey: ['feed'] })
    },
  })
}

function useProblemMutation<TInput>(code: string, request: (input: TInput) => Promise<ProblemView>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: request,
    onSuccess: (problem) => {
      queryClient.setQueryData(['problem', code], problem)
      void queryClient.invalidateQueries({ queryKey: ['feed'] })
      void queryClient.invalidateQueries({ queryKey: ['my-problems'] })
    },
  })
}

export function useTransition(code: string) {
  return useProblemMutation(code, (input: { toStatus: ProblemStatus; note?: string; evidenceMediaIds?: string[] }) =>
    apiRequest<ProblemView>(`/api/v1/problems/${encodeURIComponent(code)}/status-transitions`, { body: input, auth: true }),
  )
}

export function usePostUpdate(code: string) {
  return useProblemMutation(code, (input: { body: string; mediaIds: string[] }) =>
    apiRequest<ProblemView>(`/api/v1/problems/${encodeURIComponent(code)}/updates`, { body: input, auth: true }),
  )
}

export function useResolutionVote(code: string) {
  return useProblemMutation(code, (vote: 'CONFIRM' | 'DISPUTE') =>
    apiRequest<ProblemView>(`/api/v1/problems/${encodeURIComponent(code)}/resolution-votes`, { body: { vote }, auth: true }),
  )
}
