import { describe, expect, it } from 'vitest'
import { stripTeamSuffix } from './matchPresentation'

describe('stripTeamSuffix', () => {
  it('removes the current team suffix from a player name', () => {
    expect(stripTeamSuffix('Александр Панов (Метеор)', 'Метеор', 'Метеор Новинки'))
      .toBe('Александр Панов')
  })

  it('preserves unrelated parentheses', () => {
    expect(stripTeamSuffix('Александр Панов (капитан)', 'Метеор'))
      .toBe('Александр Панов (капитан)')
  })

  it('removes one of several exact team suffixes', () => {
    expect(stripTeamSuffix('Андрей Жуков (Олимп)', 'Сокол', 'Олимп'))
      .toBe('Андрей Жуков')
  })
})
