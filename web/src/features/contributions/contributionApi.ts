import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../../lib/api'
import type { AssistanceType } from '../../lib/labels'
import { useSession } from '../../lib/session'

export interface ContributionView {
  id: string
  contributorId: string
  contributorName: string
  types: AssistanceType[]
  solutionId: string | null
  solutionTitle: string | null
  problemCode: string
  problemTitle: string
  /** Only for the problem owner, the solution's proposer, and the contributor. */
  message: string | null
  contact: { email: string | null; phone: string | null } | null
  viewerIsContributor: boolean
  createdAt: string
}

export interface OfferInput {
  types: AssistanceType[]
  message: string
  solutionId?: string
  shareContact: boolean
}

export function useProblemContributions(code: string) {
  const signedIn = useSession((s) => s.user !== null)
  const restoring = useSession((s) => s.restoring)
  return useQuery({
    queryKey: ['contributions', code, signedIn],
    queryFn: () => apiRequest<ContributionView[]>(`/api/v1/problems/${encodeURIComponent(code)}/contributions`, { auth: signedIn }),
    enabled: !restoring,
  })
}

export function useMyContributions() {
  return useQuery({ queryKey: ['my-contributions'], queryFn: () => apiRequest<ContributionView[]>('/api/v1/me/contributions', { auth: true }) })
}

function useInvalidate() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: ['contributions'] })
    void queryClient.invalidateQueries({ queryKey: ['my-contributions'] })
  }
}

export function useOfferHelp(code: string) {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: (input: OfferInput) =>
      apiRequest<ContributionView>(`/api/v1/problems/${encodeURIComponent(code)}/contributions`, { body: input, auth: true }),
    onSuccess: invalidate,
  })
}

export function useWithdrawOffer() {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: (id: string) => apiRequest<void>(`/api/v1/contributions/${id}`, { method: 'DELETE', auth: true }),
    onSuccess: invalidate,
  })
}
