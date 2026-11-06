import { useQuery } from '@tanstack/react-query'
import { getJson } from '../../lib/api'

export interface City {
  id: number
  code: string
  name: string
  stateId: number
  stateName: string
}

export interface Locality {
  id: number
  name: string
}

export function useCities() {
  return useQuery({ queryKey: ['cities'], queryFn: () => getJson<City[]>('/api/v1/geo/cities'), staleTime: Infinity })
}

export function useLocalities(cityId: number | null) {
  return useQuery({
    queryKey: ['localities', cityId],
    queryFn: () => getJson<Locality[]>(`/api/v1/geo/cities/${cityId}/localities`),
    enabled: cityId !== null,
    staleTime: Infinity,
  })
}
