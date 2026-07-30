import { expect, test } from '@playwright/test'

const users = {
  TEAM_REP: {
    userId: 2,
    id: 2,
    email: 'rep@example.com',
    name: 'Анна Представитель',
    roles: ['TEAM_REP'],
    mustChangePassword: false,
  },
  REFEREE: {
    userId: 3,
    id: 3,
    email: 'referee@example.com',
    name: 'Иван Рефери',
    roles: ['REFEREE'],
    mustChangePassword: false,
  },
  SUPER_ADMIN: {
    userId: 1,
    id: 1,
    email: 'admin@example.com',
    name: 'Антон Администратор',
    roles: ['SUPER_ADMIN'],
    mustChangePassword: false,
  },
}

const activeSeason = {
  id: 1,
  name: 'Сезон 2026',
  status: 'ACTIVE',
  roundsCount: 2,
  regularToursCount: 10,
  playoffEnabled: false,
  maxRosterSize: 20,
  applicationOpen: true,
  applicationSubmittable: true,
  applicationStatus: 'DRAFT',
  selectedPlayersCount: 1,
}

const scheduledMatch = {
  id: 101,
  seasonName: activeSeason.name,
  tourName: 'Тур 1',
  kickoffAt: '2026-08-01T15:00:00Z',
  homeTeam: { id: 20, name: 'Альфа' },
  awayTeam: { id: 21, name: 'Бета' },
  homeLineup: {
    teamId: 20,
    teamName: 'Альфа',
    submittedAt: '2026-07-31T12:00:00Z',
    players: [
      { playerId: 201, playerName: 'Алексей Первый', sortOrder: 1, isGoalkeeper: false },
    ],
    availablePlayers: [],
  },
  awayLineup: {
    teamId: 21,
    teamName: 'Бета',
    submittedAt: '2026-07-31T12:10:00Z',
    players: [
      { playerId: 202, playerName: 'Борис Второй', sortOrder: 1, isGoalkeeper: false },
    ],
    availablePlayers: [],
  },
  availableReferees: [
    { id: 3, fullName: 'Иван Рефери', city: 'Богородск' },
  ],
  protocol: {
    status: 'SCHEDULED',
    homeScore: null,
    awayScore: null,
    events: [],
  },
}

const seasonOverview = {
  teams: [
    { id: 20, name: 'Альфа' },
    { id: 21, name: 'Бета' },
  ],
  standings: [
    {
      position: 1,
      teamId: 20,
      teamName: 'Альфа',
      matchesPlayed: 1,
      goalsFor: 2,
      goalsAgainst: 0,
      goalDifference: 2,
      points: 3,
    },
    {
      position: 2,
      teamId: 21,
      teamName: 'Бета',
      matchesPlayed: 1,
      goalsFor: 0,
      goalsAgainst: 2,
      goalDifference: -2,
      points: 0,
    },
  ],
  tours: [
    {
      id: 11,
      name: 'Тур 1',
      roundNumber: 1,
      stageType: 'REGULAR',
      matches: [
        {
          id: 101,
          homeTeamName: 'Альфа',
          awayTeamName: 'Бета',
          kickoffAt: scheduledMatch.kickoffAt,
          status: 'SCHEDULED',
          homeScore: null,
          awayScore: null,
        },
      ],
    },
  ],
  standingsConfig: {
    lastCalculatedAt: '2026-07-31T20:00:00Z',
  },
}

const teamPlayer = {
  id: 201,
  fullName: 'Алексей Первый',
  birthDate: '2000-01-01',
  isGoalkeeper: false,
  selectedForSeason: true,
  seasons: [{ id: 1, name: activeSeason.name }],
}

function json(route, payload, status = 200) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(payload),
  })
}

async function mockProductBackend(page, role = null) {
  const user = role ? users[role] : null

  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname

    if (path === '/api/auth/refresh' && user) {
      return json(route, { token: 'journey-token', ...user })
    }
    if (path === '/api/auth/me' && user) {
      return json(route, user)
    }
    if (path === '/api/admin/access/me' && user) {
      return json(route, {
        roles: user.roles,
        teamScopes: role === 'TEAM_REP'
          ? [{ teamId: 20, teamName: 'Альфа', canEditRoster: true, canEditApplication: true }]
          : [],
      })
    }

    if (path === '/api/seasons') {
      return json(route, [activeSeason])
    }
    if (path === '/api/seasons/1/overview') {
      return json(route, seasonOverview)
    }
    if (path === '/api/seasons/1/player-stats') {
      return json(route, [])
    }
    if (path === '/api/league/overview') {
      return json(route, {
        officials: [
          {
            id: 1,
            fullName: 'Сергей Руководитель',
            positionTitle: 'Руководитель лиги',
            bio: 'Организация соревнований',
          },
        ],
        venues: [],
        seasonDocuments: [],
      })
    }

    if (path === '/api/matches/101' && request.method() === 'GET') {
      return json(route, scheduledMatch)
    }
    if (path === '/api/matches/101/protocol' && request.method() === 'PUT') {
      const payload = request.postDataJSON()
      return json(route, {
        ...scheduledMatch,
        protocol: {
          ...scheduledMatch.protocol,
          ...payload,
        },
      })
    }

    if (path === '/api/team-rep/seasons') {
      return json(route, [activeSeason])
    }
    if (path === '/api/team-rep/players') {
      return json(route, [teamPlayer])
    }
    if (path === '/api/team-rep/transfers/incoming-pending') {
      return json(route, {
        totalPendingCount: 0,
        requests: [],
        pageNumber: 0,
        pageSize: 20,
        totalElements: 0,
        totalPages: 0,
      })
    }
    if (path === '/api/team-rep/seasons/1/players') {
      return json(route, {
        seasonId: 1,
        ...activeSeason,
        players: [teamPlayer],
        availablePlayers: [],
      })
    }
    if (path === '/api/team-rep/seasons/1/transfers') {
      return json(route, {
        seasonId: 1,
        seasonName: activeSeason.name,
        seasonStatus: 'ACTIVE',
        teamId: 20,
        teamName: 'Альфа',
        privilegedAccess: role === 'REFEREE' || role === 'SUPER_ADMIN',
        transferWindowOpen: true,
        selectedPlayersCount: 1,
        maxRosterSize: 20,
        sourceTeams: [{ id: 21, name: 'Бета' }],
        targetTeams: [{ id: 20, name: 'Альфа' }, { id: 21, name: 'Бета' }],
        requests: [],
        pageNumber: 0,
        pageSize: 20,
        totalElements: 0,
        totalPages: 0,
      })
    }

    if (path === '/api/admin/access/users') {
      return json(route, {
        content: [
          {
            id: 22,
            email: 'user@example.com',
            name: 'Пользователь Лиги',
            roles: ['USER'],
            mustChangePassword: false,
          },
        ],
        number: 0,
        totalPages: 1,
        totalElements: 1,
      })
    }
    if (path === '/api/admin/access/users/22') {
      return json(route, {
        userId: 22,
        email: 'user@example.com',
        name: 'Пользователь Лиги',
        roles: ['USER'],
        teamScopes: [],
        mustChangePassword: false,
      })
    }
    if (path === '/api/health') {
      return json(route, { status: 'UP' })
    }

    return json(route, [])
  })
}

async function restoreSession(page, role) {
  await mockProductBackend(page, role)
  await page.addInitScript(() => {
    localStorage.setItem('football_stats_persistent_session', '1')
  })
}

async function openAdminSection(page, sectionId, desktopLabel) {
  if ((page.viewportSize()?.width || 0) <= 860) {
    await page.getByRole('combobox', { name: 'Раздел' }).selectOption(sectionId)
    return
  }
  await page.getByRole('button', { name: desktopLabel, exact: true }).click()
}

test('болельщик: сезон → тур → матч → информация о лиге', async ({ page }) => {
  await mockProductBackend(page)
  await page.goto('/')

  await expect(page.getByRole('combobox', { name: 'Сезон' })).toHaveValue('1')
  await expect(page.locator('.standings-table-desktop')).toContainText('Альфа')

  await expect(page.getByLabel('Выбрать тур')).toHaveValue('11')
  await page.getByRole('link', { name: /Альфа.*Бета/ }).click()
  await expect(page).toHaveURL(/\/matches\/101$/)
  await expect(page.getByRole('heading', { name: 'Альфа', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Бета', exact: true })).toBeVisible()

  await page.getByRole('link', { name: 'О лиге' }).click()
  await expect(page.getByRole('heading', { name: 'Ответственные лица' })).toBeVisible()
  await expect(page.getByText('Сергей Руководитель')).toBeVisible()
})

test('представитель команды: кабинет → сезонная заявка → трансферы', async ({ page }) => {
  await restoreSession(page, 'TEAM_REP')
  await page.goto('/')

  await page.getByRole('button', { name: 'Представитель команды "Альфа"' }).click()
  await expect(page).toHaveURL(/\/team-rep-dashboard$/)
  await expect(page.getByRole('heading', { name: 'Кабинет сезонных заявок команды' })).toBeVisible()

  await page.getByLabel('Выберите сезон для просмотра заявки').selectOption('1')
  await expect(page.getByText('Заявка: Черновик')).toBeVisible()
  await page.getByRole('button', { name: 'Показать состав' }).click()
  await expect(page.getByText('Алексей Первый')).toBeVisible()

  await page.getByRole('button', { name: 'Трансферы' }).click()
  await expect(page).toHaveURL(/\/team-rep-transfers$/)
  await page.getByLabel('Выберите сезон').selectOption('1')
  await expect(page.getByText('Трансферы открыты')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Трансферных заявок пока нет' })).toBeVisible()
})

test('рефери: матч из тура → заполнение и сохранение протокола', async ({ page }) => {
  await restoreSession(page, 'REFEREE')
  await page.goto('/')

  await expect(page.getByRole('link', { name: 'Админ-панель' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'API Explorer' })).toHaveCount(0)
  await expect(page.getByLabel('Выбрать тур')).toHaveValue('11')
  await page.getByRole('link', { name: /Альфа.*Бета/ }).click()
  await expect(page).toHaveURL(/\/matches\/101$/)

  await expect(page.getByRole('heading', { name: 'Протокол матча' })).toBeVisible()
  await expect(page.getByText('REFEREE', { exact: true })).toBeVisible()

  const saveRequest = page.waitForRequest(
    (request) => request.url().endsWith('/api/matches/101/protocol') && request.method() === 'PUT',
  )
  await page.getByRole('button', { name: 'Сохранить', exact: true }).click()
  expect((await saveRequest).postDataJSON().status).toBe('FINISHED')
  await expect(page.getByText('Протокол сохранен.')).toBeVisible()
})

test('супер-админ: управление доступом → API Explorer', async ({ page }) => {
  await restoreSession(page, 'SUPER_ADMIN')
  await page.goto('/')

  await page.getByRole('link', { name: 'Админ-панель' }).click()
  await expect(page).toHaveURL(/\/admin$/)
  await openAdminSection(page, 'roles', 'Роли и доступ')

  await expect(page.getByRole('heading', { name: 'Роли и доступ' })).toBeVisible()
  await page.getByLabel('Выберите пользователя').selectOption('user@example.com')
  await page.getByRole('button', { name: 'Найти' }).click()
  await expect(page.getByText('Пользователь Лиги')).toBeVisible()
  await expect(page.locator('.admin-role-badge', { hasText: 'USER' })).toBeVisible()

  await page.getByRole('link', { name: 'API Explorer' }).click()
  await expect(page).toHaveURL(/\/api-explorer$/)
  await expect(page.getByRole('heading', { name: 'API Explorer' })).toBeVisible()
  await expect(page.locator('.api-endpoint-row')).toHaveCount(97)
})
