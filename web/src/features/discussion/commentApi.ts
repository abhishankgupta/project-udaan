import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../../lib/api'
import { useSession } from '../../lib/session'

export interface CommentView {
  id: string
  authorId: string | null
  authorName: string | null
  body: string | null
  removed: boolean
  viewerIsAuthor: boolean
  createdAt: string
  replies: CommentView[]
}

/** A discussion belongs to a problem (by public code) or a solution (by id). */
export type DiscussionTarget = { kind: 'problem'; code: string } | { kind: 'solution'; id: string }

function basePath(target: DiscussionTarget) {
  return target.kind === 'problem'
    ? `/api/v1/problems/${encodeURIComponent(target.code)}/comments`
    : `/api/v1/solutions/${target.id}/comments`
}

const key = (target: DiscussionTarget) => ['comments', target.kind, target.kind === 'problem' ? target.code : target.id]

export function useComments(target: DiscussionTarget) {
  const signedIn = useSession((s) => s.user !== null)
  const restoring = useSession((s) => s.restoring)
  return useQuery({
    queryKey: [...key(target), signedIn],
    queryFn: () => apiRequest<CommentView[]>(basePath(target), { auth: signedIn }),
    enabled: !restoring,
  })
}

export function usePostComment(target: DiscussionTarget) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { body: string; parentId?: string }) => apiRequest<CommentView>(basePath(target), { body: input, auth: true }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: key(target) })
      if (target.kind === 'problem') void queryClient.invalidateQueries({ queryKey: ['problem', target.code] })
    },
  })
}

export function useDeleteComment(target: DiscussionTarget) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => apiRequest<void>(`/api/v1/comments/${id}`, { method: 'DELETE', auth: true }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: key(target) }),
  })
}
