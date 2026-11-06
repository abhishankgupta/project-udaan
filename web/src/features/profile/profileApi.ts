import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest, getJson } from '../../lib/api'
import { useSession } from '../../lib/session'

export interface MyProfile {
  id: string
  displayName: string
  email: string | null
  phone: string | null
  roles: string[]
  bio: string | null
  city: string | null
  locality: string | null
  skills: string[]
  memberSince: string
}

export interface PublicProfile {
  id: string
  displayName: string
  bio: string | null
  city: string | null
  locality: string | null
  skills: string[]
  memberSince: string
}

export interface ProfileUpdate {
  displayName: string
  bio: string
  city: string
  locality: string
  skills: string[]
}

export function useMyProfile() {
  const signedIn = useSession((s) => s.user !== null)
  return useQuery({
    queryKey: ['me'],
    queryFn: () => apiRequest<MyProfile>('/api/v1/me', { auth: true }),
    enabled: signedIn,
  })
}

export function useUpdateMyProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (update: ProfileUpdate) => apiRequest<MyProfile>('/api/v1/me', { method: 'PATCH', body: update, auth: true }),
    onSuccess: (profile) => {
      queryClient.setQueryData(['me'], profile)
      useSession.getState().updateUser({ displayName: profile.displayName })
    },
  })
}

export function usePublicProfile(id: string) {
  return useQuery({
    queryKey: ['users', id],
    queryFn: () => getJson<PublicProfile>(`/api/v1/users/${id}`),
  })
}
