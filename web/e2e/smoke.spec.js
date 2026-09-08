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
  REFEREE: {
    userId: 3,
    email: 'referee@example.com',
    name: 'Referee',
    roles: ['REFEREE'],
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
  // Register the backend mock on the browser context so admin links opened in
  // a new tab inherit the same authenticated API responses.
  await page.context().route('http://127.0.0.1:8080/api/**', async (route) => {
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

async function mockPublicNotifications(page) {
  const requests = []
  await page.route('**/api/notifications/public?**', async (route) => {
    const url = new URL(route.request().url())
    requests.push({
      pageNumber: url.searchParams.get('pagenum'),
      pageSize: url.searchParams.get('pagesize'),
    })
    await route.fulfill({
      json: {
        items: [
          {
            id: 41,
            eventType: 'SEASON_STARTED',
            title: 'Сезон начался',
            summary: 'Опубликовано расписание.',
            body: '<p>Полный текст публичного уведомления.</p>',
            severity: 'IMPORTANT',
            createdAt: '2026-09-06T09:30:00Z',
          },
          {
            id: 40,
            eventType: 'TOUR_PUBLISHED',
            title: 'Опубликован тур',
            summary: 'Доступны новые матчи.',
            body: '<p>Расписание тура доступно.</p>',
            severity: 'INFO',
            createdAt: '2026-09-05T09:30:00Z',
          },
        ],
        pageNumber: 0,
        pageSize: 10,
        totalElements: 2,
        totalPages: 1,
      },
    })
  })
  return requests
}

test('opens the public home page', async ({ page }) => {
  await mockBackend(page)
  await page.goto('/')

  await expect(page.getByText('Футбол Богородск', { exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Туры' })).toBeVisible()
})

test('keeps guest notification read state and allows reopening it', async ({ page }) => {
  await mockBackend(page)
  const publicRequests = await mockPublicNotifications(page)
  await page.addInitScript(() => {
    if (!window.sessionStorage.getItem('guest-notification-test-ready')) {
      window.localStorage.removeItem('football_stats_public_notifications_read_v1')
      window.sessionStorage.setItem('guest-notification-test-ready', '1')
    }
  })

  await page.goto('/')
  const bell = page.getByRole('button', { name: 'Публичные объявления: новых 2' })
  await expect(bell).toBeVisible()
  await bell.click()
  await page.locator('.notification-item-main').first().click()
  await expect(page.getByRole('heading', { name: 'Сезон начался' })).toBeVisible()
  await page.getByRole('button', { name: 'Закрыть' }).click()

  const bellWithOneNew = page.getByRole('button', { name: 'Публичные объявления: новых 1' })
  await expect(bellWithOneNew).toBeVisible()
  await bellWithOneNew.click()
  await page.locator('.notification-item-main').first().click()
  await expect(page.getByText('Полный текст публичного уведомления.')).toBeVisible()
  expect(publicRequests).toEqual([{ pageNumber: '0', pageSize: '10' }])

  await page.reload()
  await expect(page.getByRole('button', { name: 'Публичные объявления: новых 1' })).toBeVisible()
  expect(publicRequests).toEqual([
    { pageNumber: '0', pageSize: '10' },
    { pageNumber: '0', pageSize: '10' },
  ])
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

  if ((page.viewportSize()?.width || 0) <= 860) {
    const sectionMenu = page.getByRole('combobox', { name: 'Раздел' })
    await expect(sectionMenu).toBeVisible()
    await expect(page.locator('.admin-navigation-desktop')).toBeHidden()

    const layout = await page.evaluate(() => ({
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
    }))
    expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewportWidth)

    await sectionMenu.selectOption('teams')
    await expect(sectionMenu).toHaveValue('teams')
    await expect(page.getByRole('heading', { name: 'Команды и составы' })).toBeVisible()

    const applicationsPagePromise = page.waitForEvent('popup')
    await sectionMenu.selectOption('season-applications')
    const applicationsPage = await applicationsPagePromise
    await expect(applicationsPage).toHaveURL(/\/season-applications-review$/)
    await applicationsPage.close()
    await expect(sectionMenu).toHaveValue('teams')
  } else {
    await expect(page.getByRole('complementary', { name: 'Разделы админ-панели' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Роли и доступ' })).toBeVisible()
    await page.getByRole('button', { name: 'Команды' }).click()
    await expect(page.getByRole('heading', { name: 'Команды и составы' })).toBeVisible()

    const applicationsPagePromise = page.waitForEvent('popup')
    await page.getByRole('button', { name: 'Заявки на сезон' }).click()
    const applicationsPage = await applicationsPagePromise
    await expect(applicationsPage).toHaveURL(/\/season-applications-review$/)
    await applicationsPage.close()
    await expect(page.getByRole('button', { name: 'Команды', exact: true }))
      .toHaveAttribute('aria-current', 'page')
  }
})

test('shows referee tools without access management', async ({ page }) => {
  await restoreSession(page, 'REFEREE')
  await page.goto('/admin')

  await expect(page).toHaveURL(/\/admin$/)
  const navigation = page.getByRole('complementary', { name: 'Разделы админ-панели' })
  await expect(navigation).toContainText('Лига')
  await expect(navigation).not.toContainText('Роли и доступ')
  await expect(navigation).not.toContainText('Представители')
  await expect(navigation).not.toContainText('Блокировки')
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

test('shows a useful empty state for transfers', async ({ page }) => {
  await mockBackend(page)
  await page.goto('/transfers')

  await expect(page.getByRole('heading', { name: 'Нет активного сезона' })).toBeVisible()
  await expect(page.getByText('Список трансферов станет доступен после открытия сезона.')).toBeVisible()
})

test('requires confirmation before banning a user', async ({ page }) => {
  await restoreSession(page, 'SUPER_ADMIN')
  await page.goto('/admin')

  if ((page.viewportSize()?.width || 0) <= 860) {
    await page.getByRole('combobox', { name: 'Раздел' }).selectOption('ban')
  } else {
    await page.getByRole('button', { name: 'Блокировки' }).click()
  }

  await page.getByLabel('Email пользователя').fill('blocked@example.com')
  await page.getByLabel('Причина').fill('Нарушение правил')
  await page.getByRole('button', { name: 'Заблокировать пользователя' }).click()

  const dialog = page.getByRole('alertdialog')
  await expect(dialog).toBeVisible()
  await expect(dialog.getByRole('heading', { name: 'Заблокировать пользователя?' })).toBeVisible()
  await dialog.getByRole('button', { name: 'Отмена' }).click()
  await expect(dialog).toBeHidden()
  await expect(page.getByText('Пользователь заблокирован.')).toHaveCount(0)

  await page.getByRole('button', { name: 'Заблокировать пользователя' }).click()
  await dialog.getByRole('button', { name: 'Заблокировать' }).click()
  await expect(page.getByText('Пользователь заблокирован.')).toBeVisible()
})

test('paginates admin notification history and reveals text on demand', async ({ page }, testInfo) => {
  await restoreSession(page, 'SUPER_ADMIN')
  const requests = []
  await page.route('**/api/notifications/admin?**', async (route) => {
    const url = new URL(route.request().url())
    const pageNumber = Number(url.searchParams.get('pagenum'))
    const pageSize = Number(url.searchParams.get('pagesize'))
    requests.push({ pageNumber, pageSize })
    const all = Array.from({ length: 11 }, (_, index) => ({
      id: 11 - index, title: `Объявление ${11 - index}`, severity: 'INFO',
      body: `<p>Полное содержание сообщения ${11 - index}</p>`,
      audienceValue: 'Все посетители сайта', createdAt: '2026-09-06T09:30:00Z',
      recipientCount: 15, readCount: 8, acknowledgedCount: 3,
    }))
    await route.fulfill({ json: {
      items: all.slice(pageNumber * pageSize, (pageNumber + 1) * pageSize),
      pageNumber, pageSize, totalElements: 11, totalPages: 2,
    } })
  })
  await page.goto('/admin')
  if ((page.viewportSize()?.width || 0) <= 860) {
    await page.getByRole('combobox', { name: 'Раздел' }).selectOption('notifications')
  } else {
    await page.getByRole('button', { name: 'Оповещения', exact: true }).click()
  }
  await page.getByRole('button', { name: /История.*Охват/ }).click()
  const rows = page.locator('.notification-admin-history-item')
  await expect(rows).toHaveCount(10)
  await expect(page.getByText('Полное содержание сообщения 11', { exact: true })).toBeHidden()
  await expect(page.getByRole('button', { name: '← Назад', exact: true })).toBeDisabled()
  await rows.first().locator('summary').click()
  await expect(page.getByText('Полное содержание сообщения 11', { exact: true })).toBeVisible()
  await rows.first().locator('summary').click()
  await expect(page.getByText('Полное содержание сообщения 11', { exact: true })).toBeHidden()
  const width = await page.evaluate(() => ({ viewport: innerWidth, content: document.documentElement.scrollWidth }))
  expect(width.content).toBeLessThanOrEqual(width.viewport)
  await page.locator('.admin-notifications-panel').screenshot({ path: testInfo.outputPath('notification-history.png') })
  await page.getByRole('button', { name: 'Далее →', exact: true }).click()
  await expect(rows).toHaveCount(1)
  await expect(rows.first().locator('summary')).toContainText('Объявление 1')
  await expect(page.getByText('11–11 из 11', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Далее →', exact: true })).toBeDisabled()
  await page.getByRole('button', { name: '← Назад', exact: true }).click()
  await expect(rows).toHaveCount(10)
  expect(requests).toEqual([
    { pageNumber: 0, pageSize: 10 }, { pageNumber: 1, pageSize: 10 }, { pageNumber: 0, pageSize: 10 },
  ])
})
