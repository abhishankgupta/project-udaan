import { describe, expect, it } from 'vitest'
import { safeNext } from './safeNext'

describe('safeNext', () => {
  it('keeps same-site paths', () => {
    expect(safeNext('/problems/new')).toBe('/problems/new')
  })

  it('rejects other sites and empty values', () => {
    expect(safeNext('https://evil.example')).toBe('/')
    expect(safeNext('//evil.example')).toBe('/')
    expect(safeNext(null)).toBe('/')
  })
})
