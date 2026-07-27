import { computed, unref } from 'vue'

export const PLAYOFF_TEAM_OPTIONS = [4, 8, 16]

export const TIE_BREAKER_RULE_OPTIONS = [
  { value: 'GOAL_DIFFERENCE', label: 'Разница мячей' },
  { value: 'GOALS_FOR', label: 'Забитые мячи' },
  { value: 'WINS', label: 'Количество побед' },
  { value: 'HEAD_TO_HEAD', label: 'Личные встречи' },
]

export function calculateRegularToursCount(teamCount, roundsCount) {
  const normalizedTeamCount = Number(teamCount || 0)
  const normalizedRoundsCount = Number(roundsCount || 0)
  if (normalizedTeamCount < 2 || normalizedRoundsCount < 1) {
    return 0
  }

  const toursPerRound = normalizedTeamCount % 2 === 0
    ? normalizedTeamCount - 1
    : normalizedTeamCount
  return toursPerRound * normalizedRoundsCount
}

export function useAdminSeasonRules({
  form,
  refereeIds,
  selectedTeamCount,
}) {
  function normalizedTieBreakers() {
    return (form.rankingRules || [])
      .map((rule) => String(rule || '').trim())
      .filter(Boolean)
  }

  function buildRankingRulesPayload() {
    return ['POINTS', ...normalizedTieBreakers(), 'ALPHABETICAL']
  }

  function normalizeRankingRulesForForm(rawRules) {
    const tieBreakers = Array.isArray(rawRules)
      ? rawRules.filter((rule) => rule !== 'POINTS' && rule !== 'ALPHABETICAL')
      : []

    if (!tieBreakers.length) {
      return ['GOAL_DIFFERENCE', 'GOALS_FOR']
    }

    return tieBreakers.map((rule) => String(rule || '')).filter(Boolean)
  }

  function availableTieBreakerRuleOptions(index) {
    const usedRules = new Set(
      (form.rankingRules || [])
        .filter((_, ruleIndex) => ruleIndex !== index)
        .map((rule) => String(rule || '').trim())
        .filter(Boolean)
    )

    return TIE_BREAKER_RULE_OPTIONS.filter(
      (option) => !usedRules.has(option.value) || option.value === form.rankingRules[index]
    )
  }

  function addRankingRule() {
    const usedRules = new Set(normalizedTieBreakers())
    const nextRule = TIE_BREAKER_RULE_OPTIONS.find((option) => !usedRules.has(option.value))
    form.rankingRules = [...form.rankingRules, nextRule?.value || '']
  }

  function removeRankingRule(index) {
    form.rankingRules = form.rankingRules.filter((_, ruleIndex) => ruleIndex !== index)
  }

  function rankingRulesSummary() {
    const labels = normalizedTieBreakers().map((rule) => {
      return TIE_BREAKER_RULE_OPTIONS.find((option) => option.value === rule)?.label || rule
    })
    if (!labels.length) {
      return 'только очки, затем алфавит'
    }
    return `очки, затем ${labels.join(' -> ')}, затем алфавит`
  }

  function validateForm() {
    const rankingRules = normalizedTieBreakers()
    const playoffEnabled = Boolean(form.playoffEnabled)
    const playoffTeamCount = playoffEnabled && form.playoffTeamCount
      ? Number(form.playoffTeamCount)
      : 0

    if (rankingRules.length !== new Set(rankingRules).size) {
      return 'Правила таблицы не должны повторяться.'
    }
    if (playoffEnabled && playoffTeamCount > 0 && playoffTeamCount < 4 && form.thirdPlaceEnabled) {
      return 'Матч за 3 место можно включить только для плей-офф на 4 команды и больше.'
    }
    if (form.maxRosterSize && Number(form.maxRosterSize) < 1) {
      return 'Максимальный размер заявки должен быть не меньше 1.'
    }
    if (
      form.transferWindowStartDate
      && form.transferWindowEndDate
      && form.transferWindowStartDate > form.transferWindowEndDate
    ) {
      return 'Дата начала окна трансферов не может быть позже даты окончания.'
    }
    return ''
  }

  function buildPayload() {
    const roundsCount = Number(form.roundsCount || 1)
    const playoffEnabled = Boolean(form.playoffEnabled)
    const playoffTeamCount = playoffEnabled && form.playoffTeamCount
      ? Number(form.playoffTeamCount)
      : null

    return {
      name: form.name,
      roundsCount,
      playoffEnabled,
      playoffTeamCount,
      thirdPlaceEnabled: playoffEnabled ? Boolean(form.thirdPlaceEnabled) : false,
      applicationDeadline: form.applicationDeadline || null,
      status: form.status || 'ACTIVE',
      maxRosterSize: form.maxRosterSize ? Number(form.maxRosterSize) : null,
      transferWindowStartDate: form.transferWindowStartDate || null,
      transferWindowEndDate: form.transferWindowEndDate || null,
      rankingRules: buildRankingRulesPayload(),
      refereeIds: unref(refereeIds),
      yellowCardsForSuspension: Number(form.yellowCardsForSuspension || 0),
      redCardsForSuspension: Number(form.redCardsForSuspension || 0),
    }
  }

  const regularToursCount = computed(() => {
    return calculateRegularToursCount(unref(selectedTeamCount), Number(form.roundsCount || 1))
  })

  const isCreateDisabled = computed(() => {
    return !String(form.name || '').trim() || Number(unref(selectedTeamCount)) < 1
  })

  return {
    addRankingRule,
    availableTieBreakerRuleOptions,
    buildPayload,
    isCreateDisabled,
    normalizeRankingRulesForForm,
    rankingRulesSummary,
    regularToursCount,
    removeRankingRule,
    validateForm,
  }
}
