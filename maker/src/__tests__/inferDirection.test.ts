import { describe, it, expect } from 'vitest'
import { inferDirection } from '../components/MapCanvas'

describe('inferDirection', () => {
  it('returns null for zero delta', () => {
    expect(inferDirection(0, 0)).toBeNull()
  })

  it('returns NORTH for positive dy', () => {
    expect(inferDirection(0, 1)).toBe('NORTH')
    expect(inferDirection(0, 5)).toBe('NORTH')
  })

  it('returns SOUTH for negative dy', () => {
    expect(inferDirection(0, -1)).toBe('SOUTH')
    expect(inferDirection(0, -3)).toBe('SOUTH')
  })

  it('returns EAST for positive dx', () => {
    expect(inferDirection(1, 0)).toBe('EAST')
    expect(inferDirection(4, 0)).toBe('EAST')
  })

  it('returns WEST for negative dx', () => {
    expect(inferDirection(-1, 0)).toBe('WEST')
    expect(inferDirection(-2, 0)).toBe('WEST')
  })

  it('returns NORTHEAST for positive dx and positive dy', () => {
    expect(inferDirection(1, 1)).toBe('NORTHEAST')
    expect(inferDirection(3, 2)).toBe('NORTHEAST')
  })

  it('returns NORTHWEST for negative dx and positive dy', () => {
    expect(inferDirection(-1, 1)).toBe('NORTHWEST')
    expect(inferDirection(-2, 5)).toBe('NORTHWEST')
  })

  it('returns SOUTHEAST for positive dx and negative dy', () => {
    expect(inferDirection(1, -1)).toBe('SOUTHEAST')
    expect(inferDirection(2, -3)).toBe('SOUTHEAST')
  })

  it('returns SOUTHWEST for negative dx and negative dy', () => {
    expect(inferDirection(-1, -1)).toBe('SOUTHWEST')
    expect(inferDirection(-4, -1)).toBe('SOUTHWEST')
  })

  it('infers direction for non-unit deltas (rooms far apart)', () => {
    // Even for non-adjacent rooms, direction is based on sign
    expect(inferDirection(10, 0)).toBe('EAST')
    expect(inferDirection(0, -10)).toBe('SOUTH')
    expect(inferDirection(5, 3)).toBe('NORTHEAST')
  })
})
