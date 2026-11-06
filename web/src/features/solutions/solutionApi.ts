import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../../lib/api'
import type { ProblemStatus } from '../../lib/labels'
import { useSession } from '../../lib/session'

export type SolutionStatus = 'PUBLISHED' | 'ACCEPTED' | 'WITHDRAWN'

export interface SolutionView {
  id: string
  problem: { id: string; publicCode: string; title: string; status: ProblemStatus }
  title: string
  summary: string
  approach: string
  estimatedCostInr: number | null
  timelineText: string | null
  resourcesRequired: string | null
  skills: string[]
  status: SolutionStatus
  proposer: { id: string; displayName: string }
  endorsements: number
  viewerEndorsed: boolean
  viewerIsProposer: boolean
  viewerIsProblemOwner: boolean
  canAccept: boolean
  createdAt: string
}

export interface SolutionDraft {
  title: string
  summary: string
  approach: string
  estimatedCostInr: number | null
  timelineText: string
  resourcesRequired: string
  skills: string[]
}

function useViewer() {
  const signedIn = useSession((s) => s.user !== null)
  const restoring = useSession((s) => s.restoring)
  return { signedIn, restoring }
}

export function useProblemSolutions(code: string) {
  const { signedIn, restoring } = useViewer()
  return useQuery({
    queryKey: ['solutions', code, signedIn],
    queryFn: () => apiRequest<SolutionView[]>(`/api/v1/problems/${encodeURIComponent(code)}/solutions`, { auth: signedIn }),
    enabled: !restoring,
  })
}

export function useSolution(id: string) {
  const { signedIn, restoring } = useViewer()
  return useQuery({
    queryKey: ['solution', id, signedIn],
    queryFn: () => apiRequest<SolutionView>(`/api/v1/solutions/${id}`, { auth: signedIn }),
    enabled: !restoring,
  })
}

export function useRecentSolutions() {
  const { signedIn, restoring } = useViewer()
  return useQuery({
    queryKey: ['recent-solutions', signedIn],
    queryFn: () => apiRequest<SolutionView[]>('/api/v1/solutions?size=30', { auth: signedIn }),
    enabled: !restoring,
  })
}

export function useMySolutions() {
  return useQuery({ queryKey: ['my-solutions'], queryFn: () => apiRequest<SolutionView[]>('/api/v1/me/solutions', { auth: true }) })
}

/** Refetches everything a solution change can affect: lists, the solution, and its problem's status. */
function useInvalidateSolutionData() {
  const queryClient = useQueryClient()
  return (problemCode: string) => {
    void queryClient.invalidateQueries({ queryKey: ['solutions', problemCode] })
    void queryClient.invalidateQueries({ queryKey: ['solution'] })
    void queryClient.invalidateQueries({ queryKey: ['problem', problemCode] })
    void queryClient.invalidateQueries({ queryKey: ['recent-solutions'] })
    void queryClient.invalidateQueries({ queryKey: ['my-solutions'] })
  }
}

export function useProposeSolution(problemCode: string) {
  const invalidate = useInvalidateSolutionData()
  return useMutation({
    mutationFn: (draft: SolutionDraft) =>
      apiRequest<SolutionView>(`/api/v1/problems/${encodeURIComponent(problemCode)}/solutions`, { body: draft, auth: true }),
    onSuccess: () => invalidate(problemCode),
  })
}

export function useSolutionAction(solution: Pick<SolutionView, 'id' | 'problem'>, action: 'accept' | 'withdraw') {
  const invalidate = useInvalidateSolutionData()
  return useMutation({
    mutationFn: () => apiRequest<SolutionView>(`/api/v1/solutions/${solution.id}/${action}`, { method: 'POST', body: {}, auth: true }),
    onSuccess: () => invalidate(solution.problem.publicCode),
  })
}

export function useEndorse(solution: Pick<SolutionView, 'id' | 'problem'>) {
  const invalidate = useInvalidateSolutionData()
  return useMutation({
    mutationFn: (on: boolean) =>
      apiRequest<{ active: boolean; count: number }>(`/api/v1/solutions/${solution.id}/endorsement`, {
        method: on ? 'PUT' : 'DELETE',
        auth: true,
      }),
    onSuccess: () => invalidate(solution.problem.publicCode),
  })
}
