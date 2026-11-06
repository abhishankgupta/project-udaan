import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../../lib/api'
import { useSession } from '../../lib/session'

export interface NotificationItem {
  id: string
  type: string
  title: string
  body: string | null
  link: string
  read: boolean
  createdAt: string
}

export interface Inbox {
  items: NotificationItem[]
  unreadCount: number
}

/** Polled once a minute while signed in, for the bell in the header. */
export function useUnreadCount() {
  const signedIn = useSession((s) => s.user !== null)
  return useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: () => apiRequest<{ unreadCount: number }>('/api/v1/notifications/unread-count', { auth: true }),
    enabled: signedIn,
    refetchInterval: 60_000,
    refetchOnWindowFocus: true,
  })
}

export function useInbox() {
  return useQuery({ queryKey: ['notifications', 'inbox'], queryFn: () => apiRequest<Inbox>('/api/v1/notifications?size=50', { auth: true }) })
}

export function useMarkRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ids?: string[]) =>
      apiRequest<{ unreadCount: number }>('/api/v1/notifications/read', { body: ids ? { ids } : {}, auth: true }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })
}
