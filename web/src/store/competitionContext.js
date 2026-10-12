import { ref, watch } from 'vue'

const STORAGE_KEY = 'football_stats_competition_context_v1'
const stored = readStored()
const seasonId = ref(stored.seasonId)
const competitionId = ref(stored.competitionId)

function normalizeId(value) {
  const normalized = String(value ?? '').trim()
  return /^\d+$/.test(normalized) ? normalized : ''
}

function readStored() {
  if (typeof localStorage === 'undefined') return { seasonId: '', competitionId: '' }
  try {
    const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    return { seasonId: normalizeId(value.seasonId), competitionId: normalizeId(value.competitionId) }
  } catch {
    return { seasonId: '', competitionId: '' }
  }
}

function persist() {
  if (typeof localStorage === 'undefined') return
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    seasonId: seasonId.value,
    competitionId: competitionId.value,
  }))
}

watch([seasonId, competitionId], persist)

export function chooseAvailableContext(seasons, competitions = [], requested = {}) {
  const seasonList = Array.isArray(seasons) ? seasons : []
  const requestedSeasonId = normalizeId(requested.seasonId || seasonId.value)
  const selectedSeason = seasonList.find((item) => String(item.id) === requestedSeasonId) || seasonList[0]
  const nextSeasonId = selectedSeason ? String(selectedSeason.id) : ''
  const competitionList = (Array.isArray(competitions) ? competitions : [])
    .filter((item) => !item.seasonId || String(item.seasonId) === nextSeasonId)
  const requestedCompetitionId = normalizeId(requested.competitionId || competitionId.value)
  const selectedCompetition = competitionList.find((item) => String(item.id) === requestedCompetitionId)
    || competitionList.find((item) => item.type === 'CHAMPIONSHIP')
    || competitionList[0]
  return {
    seasonId: nextSeasonId,
    competitionId: selectedCompetition
      ? String(selectedCompetition.id)
      : (competitionList.length ? '' : requestedCompetitionId),
  }
}

export function useCompetitionContext(route = null, router = null) {
  if (route?.query) {
    const routeSeason = normalizeId(route.query.season)
    const routeCompetition = normalizeId(route.query.competition)
    if (routeSeason) seasonId.value = routeSeason
    if (routeCompetition) competitionId.value = routeCompetition
  }

  function selectAvailable(seasons, competitions = []) {
    const selected = chooseAvailableContext(seasons, competitions, {
      seasonId: seasonId.value,
      competitionId: competitionId.value,
    })
    seasonId.value = selected.seasonId
    competitionId.value = selected.competitionId
    syncUrl()
    return selected
  }

  function syncUrl() {
    if (!router || !route) return
    const query = { ...route.query }
    delete query.season
    delete query.competition
    persist()
    if ('season' in route.query || 'competition' in route.query) void router.replace({ query })
  }

  return { seasonId, competitionId, selectAvailable, syncUrl }
}

export function cleanCompetitionUrl(to) {
  const season = normalizeId(to.query.season)
  const competition = normalizeId(to.query.competition)
  if (season) {
    if (season !== seasonId.value) competitionId.value = ''
    seasonId.value = season
  }
  if (competition) competitionId.value = competition
  if (!('season' in to.query || 'competition' in to.query)) return
  persist()
  const query = { ...to.query }
  delete query.season
  delete query.competition
  return { path: to.path, query, hash: to.hash, replace: true }
}
