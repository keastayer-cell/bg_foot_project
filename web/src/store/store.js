import { computed, ref } from 'vue'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

const seasons = ref([])
const season = ref('')

const teams = [
  'Север',
  'Юг',
  'Восток',
  'Центр',
  'Спартак',
  'Динамо',
  'Локомотив',
  'Олимп',
  'Волга',
  'Заря',
  'Старт',
  'Факел',
]

function createLineupPlayer(name, stats = {}) {
  return {
    name,
    goals: stats.goals || 0,
    yellowCards: stats.yellowCards || 0,
    redCards: stats.redCards || 0,
  }
}

function normalizeLineup(lineup = []) {
  return lineup.map((player) => {
    if (typeof player === 'string') {
      return createLineupPlayer(player)
    }

    return createLineupPlayer(player.name, player)
  })
}

const tours = ref([
  {
    id: 1001,
    season: '2025/26',
    title: '1 тур',
    date: '2026-04-10',
    matchIds: [55601, 55602, 55603, 55604, 55605, 55606],
  },
])

const matches = ref([
  {
    id: 55601,
    tourId: 1001,
    season: '2025/26',
    date: '2026-04-10',
    teamA: 'Север',
    teamB: 'Юг',
    status: 'played',
    applications: {
      teamA: normalizeLineup([
        { name: 'Иван Петров', goals: 1 },
        { name: 'Павел Соколов', goals: 1 },
        { name: 'Андрей Крылов', yellowCards: 1 },
        'Сергей Волков',
        'Максим Романов',
      ]),
      teamB: normalizeLineup([
        { name: 'Егор Климов', goals: 1 },
        { name: 'Олег Жданов', yellowCards: 1 },
        'Руслан Котов',
        'Никита Громов',
        'Илья Терехов',
      ]),
    },
    protocol: {
      bestPlayer: 'Иван Петров',
    },
  },
  {
    id: 55602,
    tourId: 1001,
    season: '2025/26',
    date: '2026-04-07',
    teamA: 'Восток',
    teamB: 'Центр',
    status: 'played',
    applications: {
      teamA: normalizeLineup([
        { name: 'Сергей Иванов', goals: 1 },
        { name: 'Антон Белов', yellowCards: 1 },
        'Артем Ларин',
        'Руслан Котов',
        'Илья Терехов',
      ]),
      teamB: normalizeLineup([
        { name: 'Максим Романов', goals: 2 },
        { name: 'Петр Фадеев', yellowCards: 1 },
        'Михаил Козлов',
        'Егор Панов',
        'Алексей Жданов',
      ]),
    },
    protocol: {
      bestPlayer: 'Максим Романов',
    },
  },
  {
    id: 55603,
    tourId: 1001,
    season: '2025/26',
    date: '2026-04-10',
    teamA: 'Спартак',
    teamB: 'Динамо',
    status: 'played',
    applications: {
      teamA: normalizeLineup([
        { name: 'Кирилл Гусев', goals: 3 },
        { name: 'Артем Морозов', yellowCards: 1 },
        'Иван Седов',
        'Роман Беляев',
        'Глеб Орлов',
      ]),
      teamB: normalizeLineup([
        { name: 'Даниил Брагин', goals: 1 },
        { name: 'Павел Крылов', goals: 1 },
        { name: 'Сергей Яковлев', yellowCards: 1 },
        'Игорь Титов',
        'Владислав Рябов',
      ]),
    },
    protocol: {
      bestPlayer: 'Кирилл Гусев',
    },
  },
  {
    id: 55604,
    tourId: 1001,
    season: '2025/26',
    date: '2026-04-10',
    teamA: 'Локомотив',
    teamB: 'Олимп',
    status: 'played',
    applications: {
      teamA: normalizeLineup([
        { name: 'Антон Панкратов', goals: 2 },
        { name: 'Виктор Голубев', goals: 1 },
        'Николай Матвеев',
        { name: 'Алексей Блохин', yellowCards: 1 },
        'Семен Шаров',
      ]),
      teamB: normalizeLineup([
        { name: 'Юрий Котов', goals: 1 },
        { name: 'Степан Белов', redCards: 1 },
        'Петр Мелехин',
        'Артур Панов',
        'Егор Дмитриев',
      ]),
    },
    protocol: {
      bestPlayer: 'Антон Панкратов',
    },
  },
  {
    id: 55605,
    tourId: 1001,
    season: '2025/26',
    date: '2026-04-10',
    teamA: 'Волга',
    teamB: 'Заря',
    status: 'played',
    applications: {
      teamA: normalizeLineup([
        { name: 'Роман Сычев', goals: 2 },
        { name: 'Илья Гордеев', goals: 1 },
        'Павел Минин',
        'Максим Белый',
        { name: 'Егор Шувалов', yellowCards: 1 },
      ]),
      teamB: normalizeLineup([
        { name: 'Станислав Осипов', goals: 1 },
        { name: 'Михаил Доронин', yellowCards: 1 },
        'Дмитрий Кулагин',
        'Олег Назаров',
        'Руслан Фомин',
      ]),
    },
    protocol: {
      bestPlayer: 'Роман Сычев',
    },
  },
  {
    id: 55606,
    tourId: 1001,
    season: '2025/26',
    date: '2026-04-10',
    teamA: 'Старт',
    teamB: 'Факел',
    status: 'played',
    applications: {
      teamA: normalizeLineup([
        { name: 'Матвей Чернов', goals: 1 },
        'Кирилл Федотов',
        'Григорий Мартынов',
        { name: 'Евгений Лавров', yellowCards: 1 },
        'Арсений Попов',
      ]),
      teamB: normalizeLineup([
        { name: 'Александр Трусов', goals: 2 },
        { name: 'Никита Шестаков', goals: 1 },
        'Петр Никифоров',
        'Сергей Дорофеев',
        { name: 'Иван Черкасов', yellowCards: 1 },
      ]),
    },
    protocol: {
      bestPlayer: 'Александр Трусов',
    },
  },
])

function computeScore(match, teamName) {
  const lineup = teamName === match.teamA ? match.applications.teamA : match.applications.teamB
  return normalizeLineup(lineup).reduce((sum, player) => sum + player.goals, 0)
}

function decorateMatch(match) {
  return {
    ...match,
    applications: {
      teamA: normalizeLineup(match.applications?.teamA),
      teamB: normalizeLineup(match.applications?.teamB),
    },
    scoreA: computeScore(match, match.teamA),
    scoreB: computeScore(match, match.teamB),
  }
}

const seasonTours = computed(() => {
  return tours.value
    .filter((t) => t.season === season.value)
    .sort((a, b) => new Date(a.date) - new Date(b.date))
})

const seasonMatches = computed(() => {
  return matches.value
    .filter((m) => m.season === season.value)
    .map((m) => decorateMatch(m))
    .sort((a, b) => new Date(a.date) - new Date(b.date))
})

const playedSeasonMatches = computed(() => {
  return seasonMatches.value.filter((m) => m.status === 'played')
})

const standings = computed(() => {
  const table = new Map()

  function getTeam(name) {
    if (!table.has(name)) {
      table.set(name, {
        team: name,
        played: 0,
        wins: 0,
        draws: 0,
        losses: 0,
        gf: 0,
        ga: 0,
        points: 0,
      })
    }
    return table.get(name)
  }

  for (const m of playedSeasonMatches.value) {
    const a = getTeam(m.teamA)
    const b = getTeam(m.teamB)

    a.played += 1
    b.played += 1
    a.gf += m.scoreA
    a.ga += m.scoreB
    b.gf += m.scoreB
    b.ga += m.scoreA

    if (m.scoreA > m.scoreB) {
      a.wins += 1
      b.losses += 1
      a.points += 3
    } else if (m.scoreA < m.scoreB) {
      b.wins += 1
      a.losses += 1
      b.points += 3
    } else {
      a.draws += 1
      b.draws += 1
      a.points += 1
      b.points += 1
    }
  }

  return Array.from(table.values())
    .sort((x, y) => {
      if (y.points !== x.points) return y.points - x.points
      const gdX = x.gf - x.ga
      const gdY = y.gf - y.ga
      if (gdY !== gdX) return gdY - gdX
      if (y.gf !== x.gf) return y.gf - x.gf
      return x.team.localeCompare(y.team, 'ru')
    })
    .map((row, idx) => ({ ...row, rank: idx + 1 }))
})

const topPlayers = computed(() => {
  const players = new Map()

  function getPlayer(name) {
    if (!players.has(name)) {
      players.set(name, { name, goals: 0, best: 0, matches: new Set(), team: '' })
    }
    return players.get(name)
  }

  for (const m of playedSeasonMatches.value) {
    const involved = new Set()

    for (const player of m.applications?.teamA || []) {
      const p = getPlayer(player.name)
      p.goals += player.goals || 0
      p.team = p.team || m.teamA
      involved.add(player.name)
    }

    for (const player of m.applications?.teamB || []) {
      const p = getPlayer(player.name)
      p.goals += player.goals || 0
      p.team = p.team || m.teamB
      involved.add(player.name)
    }

    if (m.protocol?.bestPlayer) {
      const p = getPlayer(m.protocol.bestPlayer)
      p.best += 1
      involved.add(m.protocol.bestPlayer)
    }

    for (const name of involved) {
      getPlayer(name).matches.add(m.id)
    }
  }

  return Array.from(players.values())
    .map((p) => ({ ...p, matches: p.matches.size }))
    .sort((a, b) => {
      if (b.goals !== a.goals) return b.goals - a.goals
      if (b.best !== a.best) return b.best - a.best
      return b.matches - a.matches
    })
})

function findMatchById(id) {
  const match = matches.value.find((m) => String(m.id) === String(id))
  return match ? decorateMatch(match) : null
}

function findTourById(id) {
  return tours.value.find((t) => String(t.id) === String(id))
}

function addTour(payload) {
  const newTour = {
    id: Date.now(),
    season: season.value,
    title: payload.title,
    date: payload.date,
    matchIds: [],
  }
  tours.value.push(newTour)
  return newTour
}

function addMatchToTour(payload) {
  const tour = findTourById(payload.tourId)
  if (!tour) return null

  const newMatch = {
    id: Date.now(),
    tourId: tour.id,
    season: season.value,
    date: payload.date,
    teamA: payload.teamA,
    teamB: payload.teamB,
    status: 'scheduled',
    applications: {
      teamA: [],
      teamB: [],
    },
    protocol: {
      bestPlayer: '',
    },
  }

  matches.value.push(newMatch)
  tour.matchIds.push(newMatch.id)
  return newMatch
}

function submitApplication(payload) {
  const match = matches.value.find((m) => String(m.id) === String(payload.matchId))
  if (!match) return false

  if (payload.team === match.teamA) {
    match.applications.teamA = payload.players.map((name) => createLineupPlayer(name))
  }

  if (payload.team === match.teamB) {
    match.applications.teamB = payload.players.map((name) => createLineupPlayer(name))
  }

  if (match.applications.teamA.length && match.applications.teamB.length) {
    match.status = 'lineups'
  }

  return true
}

function saveMatchStats(payload) {
  const match = matches.value.find((m) => String(m.id) === String(payload.matchId))
  if (!match) return false

  match.applications = {
    teamA: normalizeLineup(payload.teamA),
    teamB: normalizeLineup(payload.teamB),
  }
  match.protocol = {
    bestPlayer: payload.bestPlayer,
  }
  match.status = 'played'
  return true
}

async function loadSeasons() {
  try {
    const response = await fetch(`${apiBaseUrl}/api/seasons`)
    const payload = await response.json().catch(() => [])
    const seasonNames = Array.isArray(payload)
      ? payload
          .filter((item) => item && item.active)
          .map((item) => String(item.name || '').trim())
          .filter(Boolean)
      : []

    seasons.value = seasonNames

    if (!seasonNames.length) {
      season.value = ''
      return seasons.value
    }

    if (!seasonNames.includes(season.value)) {
      season.value = seasonNames[0]
    }

    return seasons.value
  } catch {
    seasons.value = []
    season.value = ''
    return seasons.value
  }
}

loadSeasons()

export function useStore() {
  return {
    seasons,
    season,
    teams,
    tours,
    matches,
    seasonTours,
    seasonMatches,
    standings,
    topPlayers,
    findMatchById,
    findTourById,
    addTour,
    addMatchToTour,
    submitApplication,
    saveMatchStats,
    loadSeasons,
  }
}
