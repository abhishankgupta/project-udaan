import { config } from './config'

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
}

export async function getJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${config.apiBaseUrl}${path}`, {
    ...init,
    headers: { Accept: 'application/json', ...init?.headers },
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
  return (await response.json()) as T
}
