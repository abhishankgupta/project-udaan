import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
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
  timeline: { type: string; fromStatus: ProblemStatus | null; toStatus: ProblemStatus | null; body: string | null; at: string }[]
  viewerIsOwner: boolean
  createdAt: string
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
