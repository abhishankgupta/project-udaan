import { useQuery } from '@tanstack/react-query'
import { getJson } from '../../lib/api'

export interface Category {
  id: string
  code: string
  nameEn: string
  nameHi: string
}

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => getJson<Category[]>('/api/v1/categories'),
    staleTime: 60 * 60 * 1000,
  })
}
