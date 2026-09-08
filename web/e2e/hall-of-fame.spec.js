import { expect, test } from '@playwright/test'

const entry = {
  competitionId: 11,
  seasonId: 1,
  seasonName: 'Демо-лига 2026',
  competitionName: 'Чемпионат',
  competitionType: 'CHAMPIONSHIP',
  formation: '1-2-1',
  awards: [
    { id: 1, code: 'CHAMPION', title: 'Чемпион', winnerType: 'TEAM', teamId: 1, winnerName: 'Атлетик Богородск' },
    { id: 2, code: 'SILVER', title: 'Серебряный призёр', winnerType: 'TEAM', teamId: 2, winnerName: 'Волна Дуденево' },
    { id: 3, code: 'BRONZE', title: 'Бронзовый призёр', winnerType: 'TEAM', teamId: 3, winnerName: 'Заря Кудьма' },
    { id: 4, code: 'BEST_PLAYER', title: 'Лучший игрок', winnerType: 'PLAYER', playerId: 5, teamId: 1, winnerName: 'Александр Белов', teamName: 'Атлетик Богородск', photoDataUrl: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==' },
    { id: 5, code: 'TOP_SCORER', title: 'Лучший бомбардир', winnerType: 'PLAYER', playerId: 6, teamId: 2, winnerName: 'Иван Орлов', teamName: 'Волна Дуденево', statValue: 9 },
  ],
  slots: [
    { id: 1, playerId: 1, playerName: 'Максим Соколов', teamId: 1, teamName: 'Атлетик Богородск', teamShortName: 'Атлетик', positionLabel: 'ВР', xPercent: 50, yPercent: 88 },
    { id: 2, playerId: 2, playerName: 'Алексей Панов', teamId: 3, teamName: 'Заря Кудьма', teamShortName: 'Заря', positionLabel: 'ЗАЩ', xPercent: 34, yPercent: 65 },
    { id: 3, playerId: 3, playerName: 'Дмитрий Волков', teamId: 2, teamName: 'Волна Дуденево', teamShortName: 'Волна', positionLabel: 'ЗАЩ', xPercent: 66, yPercent: 65 },
    { id: 4, playerId: 4, playerName: 'Илья Морозов', teamId: 1, teamName: 'Атлетик Богородск', teamShortName: 'Атлетик', positionLabel: 'НАП', xPercent: 50, yPercent: 24 },
  ],
}

test('hall of fame shows podium, automatic scorer and a responsive symbolic team', async ({ page }) => {
  const cupEntry = { ...entry, competitionId: 12, competitionName: 'Кубок осени', competitionType: 'CUP', formation: null, awards: [], slots: [] }
  const previousEntry = { ...entry, competitionId: 21, seasonId: 2, seasonName: 'Демо-лига 2025', competitionName: 'Чемпионат 2025', awards: [], slots: [] }
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    await route.fulfill({ json: path === '/api/hall-of-fame' ? [entry, cupEntry, previousEntry] : [] })
  })

  await page.goto('/hall-of-fame')
  await expect(page.getByRole('heading', { name: 'Зал славы' })).toBeVisible()
  await expect(page.getByText('Атлетик Богородск', { exact: true }).first()).toBeVisible()
  await expect(page.getByText('Лучший бомбардир', { exact: true })).toBeVisible()
  await expect(page.locator('.hall-award-stat')).toContainText('9')
  await expect(page.locator('.hall-award-stat')).toContainText('голов')
  await expect(page.locator('.hall-player')).toHaveCount(4)
  await expect(page.locator('.hall-pitch')).toHaveAttribute('aria-label', 'Символическая сборная: Чемпионат')
  await expect(page.locator('.hall-podium a').first()).toHaveAttribute('href', /\/teams\/\d+/)
  await expect(page.locator('.hall-award-card .hall-person-link').first()).toHaveAttribute('href', '/players/5?from=hall-of-fame')
  await expect(page.locator('.hall-player-label').first().getByText('Соколов М.', { exact: true })).toHaveAttribute('href', '/players/1?from=hall-of-fame')
  await expect(page.locator('.hall-player-team').first()).toHaveText('Атлетик')
  await expect(page.locator('.hall-award-avatar img')).toHaveCount(1)
  await expect(page.getByRole('combobox', { name: 'Сезон' })).toHaveValue('1')
  await expect(page.getByRole('combobox', { name: 'Турнир' })).toHaveValue('11')
  await page.getByRole('combobox', { name: 'Турнир' }).selectOption('12')
  await expect(page.getByRole('heading', { name: 'Кубок осени' })).toBeVisible()
  await page.getByRole('combobox', { name: 'Сезон' }).selectOption('2')
  await expect(page.getByRole('combobox', { name: 'Турнир' })).toHaveValue('21')
  await expect(page.getByRole('heading', { name: 'Чемпионат 2025' })).toBeVisible()

  const layout = await page.evaluate(() => ({ viewport: innerWidth, content: document.documentElement.scrollWidth }))
  expect(layout.content).toBeLessThanOrEqual(layout.viewport)
})

test('hall of fame admin limits player choices and searches by full name', async ({ page }) => {
  const candidates = Array.from({ length: 25 }, (_, index) => ({
    playerId: index + 1,
    playerName: `Игрок ${String(index + 1).padStart(2, '0')} Тестов`,
    position: 'MIDFIELDER',
    teamId: index % 2 + 1,
    teamName: index % 2 ? 'Волна Дуденево' : 'Атлетик Богородск',
  }))

  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    if (path === '/api/auth/refresh' || path === '/api/auth/me') {
      await route.fulfill({ json: { token: 'test-token', userId: 1, id: 1, email: 'admin@example.com', name: 'Admin', roles: ['SUPER_ADMIN'], mustChangePassword: false } })
      return
    }
    if (path === '/api/admin/access/me') {
      await route.fulfill({ json: { roles: ['SUPER_ADMIN'], teamScopes: [] } })
      return
    }
    if (path === '/api/seasons') {
      await route.fulfill({ json: [{ id: 1, name: 'Демо-лига 2026' }] })
      return
    }
    if (path === '/api/seasons/1/competitions' && request.method() === 'GET') {
      await route.fulfill({ json: [{ id: 11, name: 'Чемпионат', type: 'CHAMPIONSHIP' }] })
      return
    }
    if (path === '/api/seasons/1/competitions/11/honors') {
      await route.fulfill({ json: { ...entry, playersOnField: 5, published: false, awards: [], slots: [], teams: [{ id: 1, name: 'Атлетик Богородск' }, { id: 2, name: 'Волна Дуденево' }], players: candidates } })
      return
    }
    await route.fulfill({ json: [] })
  })
  await page.addInitScript(() => localStorage.setItem('football_stats_persistent_session', '1'))

  await page.goto('/admin')
  if ((page.viewportSize()?.width || 0) <= 860) {
    await page.getByRole('combobox', { name: 'Раздел' }).selectOption('league')
  } else {
    await page.getByRole('button', { name: 'Лига', exact: true }).click()
  }
  await page.getByRole('button', { name: /Зал славы/ }).click()
  await page.getByRole('combobox', { name: 'Соревнование' }).selectOption('11')

  const bestPlayerRow = page.locator('.honors-award-row').nth(3)
  const playerSearch = bestPlayerRow.getByRole('textbox', { name: 'Победитель' })
  await playerSearch.click()
  await expect(bestPlayerRow.locator('.searchable-select-option')).toHaveCount(10)
  await expect(bestPlayerRow.getByText('Показаны первые 10 из 25')).toBeVisible()
  await playerSearch.fill('Игрок 17')
  await expect(bestPlayerRow.locator('.searchable-select-option')).toHaveCount(1)
  await expect(bestPlayerRow.getByText('Игрок 17 Тестов', { exact: true })).toBeVisible()
})
