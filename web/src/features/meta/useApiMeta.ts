import { useQuery } from '@tanstack/react-query'
import { getJson } from '../../lib/api'

export interface ApiMeta {
  name: string
  environment: string
  version: string
}

export function useApiMeta() {
  return useQuery({
    queryKey: ['meta'],
    queryFn: () => getJson<ApiMeta>('/api/v1/meta'),
  })
}
