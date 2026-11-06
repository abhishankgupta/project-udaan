import { useQuery } from '@tanstack/react-query'
import { apiRequest, getJson } from '../../lib/api'
import type { AuthResponse } from '../../lib/session'

export interface AuthOptions {
  emailEnabled: boolean
  phoneOtpEnabled: boolean
  /** No SMS provider yet: the API returns the code so it can be shown on screen (test only). */
  phoneOtpTestMode: boolean
}

export interface OtpIssued {
  phone: string
  expiresInSeconds: number
  testCode: string | null
}

export function useAuthOptions() {
  return useQuery({
    queryKey: ['auth-options'],
    queryFn: () => getJson<AuthOptions>('/api/v1/auth/options'),
    staleTime: 10 * 60 * 1000,
  })
}

export const authApi = {
  register: (body: { email: string; password: string; displayName: string }) =>
    apiRequest<AuthResponse>('/api/v1/auth/register', { body }),
  login: (body: { email: string; password: string }) => apiRequest<AuthResponse>('/api/v1/auth/login', { body }),
  requestOtp: (phone: string) => apiRequest<OtpIssued>('/api/v1/auth/otp/request', { body: { phone } }),
  verifyOtp: (body: { phone: string; code: string; displayName?: string }) =>
    apiRequest<AuthResponse>('/api/v1/auth/otp/verify', { body }),
  logout: (refreshToken: string) => apiRequest<void>('/api/v1/auth/logout', { body: { refreshToken } }),
}
