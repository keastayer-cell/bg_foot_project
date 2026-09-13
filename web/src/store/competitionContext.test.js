import { describe, expect, it } from 'vitest'
import { chooseAvailableContext } from './competitionContext'

describe('competition context', () => {
  it('keeps an available explicit season and competition', () => {
    expect(chooseAvailableContext(
      [{ id: 1 }, { id: 2 }],
      [{ id: 10, seasonId: 1, type: 'CHAMPIONSHIP' }, { id: 20, seasonId: 2, type: 'CUP' }],
      { seasonId: '2', competitionId: '20' },
    )).toEqual({ seasonId: '2', competitionId: '20' })
  })

  it('falls back to the first season and its championship', () => {
    expect(chooseAvailableContext(
      [{ id: 1 }, { id: 2 }],
      [{ id: 11, seasonId: 1, type: 'CUP' }, { id: 10, seasonId: 1, type: 'CHAMPIONSHIP' }],
      { seasonId: '99', competitionId: '98' },
    )).toEqual({ seasonId: '1', competitionId: '10' })
  })
})
