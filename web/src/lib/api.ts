import { config } from './config'
import { useSession, type AuthResponse } from './session'

/** RFC 7807 error body returned by the API (section 7.1 of the architecture doc). */
export interface ApiProblem {
  title?: string
  status?: number
  detail?: string
  code?: string
  fieldErrors?: { field: string; message: string }[]
}

export class ApiError extends Error {
  readonly status: number
  readonly problem?: ApiProblem

  constructor(status: number, problem?: ApiProblem) {
    super(problem?.detail ?? `Request failed with status ${status}`)
    this.status = status
    this.problem = problem
  }

  get code(): string | undefined {
    return this.problem?.code
  }
}

type Method = 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE'

interface RequestOptions {
  method?: Method
  body?: unknown
  /** Send the access token, refreshing it once if it has expired. */
  auth?: boolean
}

async function rawRequest<T>(path: string, method: Method, body: unknown, token: string | null): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(`${config.apiBaseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    let problem: ApiProblem | undefined
    try {
      problem = (await response.json()) as ApiProblem
    } catch {
      problem = undefined
    }
    throw new ApiError(response.status, problem)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

let refreshInFlight: Promise<boolean> | null = null

/** Swaps the stored refresh token for a new pair. Concurrent callers share one request. */
export function refreshSession(): Promise<boolean> {
  if (refreshInFlight) return refreshInFlight
  const { refreshToken } = useSession.getState()
  if (!refreshToken) return Promise.resolve(false)

  refreshInFlight = rawRequest<AuthResponse>('/api/v1/auth/refresh', 'POST', { refreshToken }, null)
    .then((auth) => {
      useSession.getState().signIn(auth)
      return true
    })
    .catch((error: unknown) => {
      // Only a definite rejection ends the session; a network blip keeps the token for next time.
      if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
        useSession.getState().signOut()
      }
      return false
    })
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? (options.body === undefined ? 'GET' : 'POST')
  if (!options.auth) return rawRequest<T>(path, method, options.body, null)

  let token = useSession.getState().accessToken
  if (!token && (await refreshSession())) token = useSession.getState().accessToken
  try {
    return await rawRequest<T>(path, method, options.body, token)
  } catch (error) {
    if (error instanceof ApiError && error.status === 401 && (await refreshSession())) {
      return rawRequest<T>(path, method, options.body, useSession.getState().accessToken)
    }
    throw error
  }
}

export function getJson<T>(path: string): Promise<T> {
  return apiRequest<T>(path)
}

/** The first validation message for a field, or the general error message. */
export function errorMessage(error: unknown, field?: string): string | undefined {
  if (!error) return undefined
  if (error instanceof ApiError) {
    if (field) return error.problem?.fieldErrors?.find((f) => f.field === field)?.message
    return error.problem?.detail ?? error.message
  }
  return field ? undefined : 'Something went wrong. Please check your connection and try again.'
}
