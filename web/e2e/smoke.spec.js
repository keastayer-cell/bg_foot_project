import { expect, test } from '@playwright/test'

const users = {
  USER: {
    userId: 10,
    email: 'user@example.com',
    name: 'Test User',
    roles: ['USER'],
    mustChangePassword: false,
  },
  SUPER_ADMIN: {
    userId: 1,
    email: 'admin@example.com',
    name: 'Admin',
    roles: ['SUPER_ADMIN'],
    mustChangePassword: false,
  },
  TEAM_REP: {
    userId: 2,
    email: 'rep@example.com',
    name: 'Representative',
    roles: ['TEAM_REP'],
    mustChangePassword: false,
  },
}

async function mockBackend(page, role = 'USER') {
  const user = users[role]
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname

    if (path === '/api/auth/login' || path === '/api/auth/refresh') {
      await route.fulfill({ json: { token: 'test-token', ...user } })
      return
    }
    if (path === '/api/auth/me') {
      await route.fulfill({ json: { ...user, id: user.userId } })
      return
    }
    if (path === '/api/admin/access/me') {
      const teamScopes = role === 'TEAM_REP'
        ? [{ teamId: 20, teamName: 'Alpha', canEditRoster: true, canEditApplication: true }]
        : []
      await route.fulfill({ json: { roles: user.roles, teamScopes } })
      return
    }
    if (path === '/api/team-rep/transfers/incoming-pending') {
      await route.fulfill({
        json: {
          totalPendingCount: 0,
          requests: [],
          pageNumber: 0,
          pageSize: 20,
          totalElements: 0,
          totalPages: 0,
        },
      })
      return
    }
    if (path === '/api/matches/101') {
      await route.fulfill({
        json: {
          id: 101,
          seasonName: 'Season 2026',
          tourName: 'Tour 1',
          kickoffAt: '2026-07-27T18:00:00Z',
          homeTeam: { id: 1, name: 'Alpha' },
          awayTeam: { id: 2, name: 'Beta' },
          homeLineup: { teamId: 1, teamName: 'Alpha', players: [], availablePlayers: [] },
          awayLineup: { teamId: 2, teamName: 'Beta', players: [], availablePlayers: [] },
          availableReferees: [],
          protocol: { status: 'SCHEDULED', events: [] },
        },
      })
      return
    }
    await route.fulfill({ json: [] })
  })
}

async function restoreSession(page, role) {
  await mockBackend(page, role)
  await page.addInitScript(() => {
    localStorage.setItem('football_stats_persistent_session', '1')
  })
}

test('opens the public home page', async ({ page }) => {
  await mockBackend(page)
  await page.goto('/')

  await expect(page.getByText('Футбол Богородск', { exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Туры' })).toBeVisible()
})

test('logs in through the real authorization form', async ({ page }) => {
  await mockBackend(page)
  await page.goto('/')
  await page.getByRole('button', { name: 'Войти / Регистрация' }).click()
  await page.getByLabel('Email').fill('user@example.com')
  await page.locator('input[type="password"]').fill('secret12')

  const loginRequest = page.waitForRequest(
    (request) => request.url().endsWith('/api/auth/login') && request.method() === 'POST',
  )
  await page.getByRole('button', { name: 'Войти', exact: true }).click()

  expect(await loginRequest.then((request) => request.postDataJSON())).toEqual({
    email: 'user@example.com',
    password: 'secret12',
  })
  await expect(page.getByRole('button', { name: 'Выйти' })).toBeVisible()
})

test('opens the admin page for SUPER_ADMIN', async ({ page }) => {
  await restoreSession(page, 'SUPER_ADMIN')
  await page.goto('/admin')

  await expect(page).toHaveURL(/\/admin$/)
  await expect(page.getByRole('heading', { name: 'Админ-панель' })).toBeVisible()
})

test('opens the team representative dashboard', async ({ page }) => {
  await restoreSession(page, 'TEAM_REP')
  await page.goto('/team-rep-dashboard')

  await expect(page).toHaveURL(/\/team-rep-dashboard$/)
  await expect(page.getByRole('heading', { name: 'Кабинет сезонных заявок команды' })).toBeVisible()
  await expect(page.getByText('Alpha', { exact: true })).toBeVisible()
})

test('opens a public match after the page decomposition', async ({ page }) => {
  await mockBackend(page)
  await page.goto('/match/101')

  await expect(page.getByRole('heading', { name: 'Alpha', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Beta', exact: true })).toBeVisible()
  await expect(page.getByText('Season 2026 · Tour 1')).toBeVisible()
})
