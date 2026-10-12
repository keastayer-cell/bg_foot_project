import { describe, expect, it } from 'vitest'
import { chooseAvailableContext, cleanCompetitionUrl, useCompetitionContext } from './competitionContext'

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

describe('clean filter URLs', () => {
  it('imports legacy filters while retaining unrelated query and hash', () => {
    expect(cleanCompetitionUrl({ path: '/', query: { season: '2', competition: '20', tab: 'table' }, hash: '#results' }))
      .toEqual({ path: '/', query: { tab: 'table' }, hash: '#results', replace: true })
    const context = useCompetitionContext()
    expect(context.seasonId.value).toBe('2')
    expect(context.competitionId.value).toBe('20')
  })
  it('does not redirect clean URLs or leak a previous tournament into another season', () => {
    expect(cleanCompetitionUrl({ path: '/hall-of-fame', query: {}, hash: '' })).toBeUndefined()
    cleanCompetitionUrl({ path: '/', query: { season: '3' }, hash: '' })
    expect(useCompetitionContext().competitionId.value).toBe('')
  })
})
