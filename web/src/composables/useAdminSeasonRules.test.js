import { reactive, ref } from 'vue'
import { describe, expect, it } from 'vitest'
import {
  calculateRegularToursCount,
  useAdminSeasonRules,
} from './useAdminSeasonRules'

function createRules(overrides = {}) {
  const form = reactive({
    name: '2026/27',
    roundsCount: '2',
    playoffEnabled: false,
    playoffTeamCount: '',
    thirdPlaceEnabled: false,
    status: 'DRAFT',
    maxRosterSize: '',
    playersOnField: '11',
    applicationDeadline: '',
    transferWindowStartDate: '',
    transferWindowEndDate: '',
    rankingRules: ['GOAL_DIFFERENCE', 'GOALS_FOR'],
    yellowCardsForSuspension: '0',
    yellowSuspensionMatches: '1',
    redCardsForSuspension: '1',
    ...overrides,
  })

  return {
    form,
    rules: useAdminSeasonRules({
      form,
      refereeIds: ref([4, 7]),
      selectedTeamCount: ref(6),
    }),
  }
}

describe('useAdminSeasonRules', () => {
  it('calculates tours for even and odd team counts', () => {
    expect(calculateRegularToursCount(6, 2)).toBe(10)
    expect(calculateRegularToursCount(5, 2)).toBe(10)
    expect(calculateRegularToursCount(1, 2)).toBe(0)
  })

  it('builds the API payload with fixed first and last ranking rules', () => {
    const { rules } = createRules({
      playoffEnabled: true,
      playoffTeamCount: '4',
      thirdPlaceEnabled: true,
      maxRosterSize: '18',
      playersOnField: '8',
    })

    expect(rules.buildPayload()).toMatchObject({
      roundsCount: 2,
      playoffTeamCount: 4,
      thirdPlaceEnabled: true,
      maxRosterSize: 18,
      playersOnField: 8,
      rankingRules: ['POINTS', 'GOAL_DIFFERENCE', 'GOALS_FOR', 'ALPHABETICAL'],
      yellowSuspensionMatches: 1,
      refereeIds: [4, 7],
    })
  })

  it('rejects duplicate rules and reversed transfer dates', () => {
    const duplicate = createRules({
      rankingRules: ['WINS', 'WINS'],
    })
    expect(duplicate.rules.validateForm()).toBe('Правила таблицы не должны повторяться.')

    const dates = createRules({
      transferWindowStartDate: '2026-09-01',
      transferWindowEndDate: '2026-08-01',
    })
    expect(dates.rules.validateForm()).toBe('Дата начала окна трансферов не может быть позже даты окончания.')
  })

  it('keeps selected options available only in their current position', () => {
    const { rules } = createRules()

    expect(rules.availableTieBreakerRuleOptions(0).map((option) => option.value))
      .not.toContain('GOALS_FOR')
    expect(rules.availableTieBreakerRuleOptions(0).map((option) => option.value))
      .toContain('GOAL_DIFFERENCE')
  })
})
