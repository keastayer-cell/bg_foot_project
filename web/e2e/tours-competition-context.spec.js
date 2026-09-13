import { test, expect } from '@playwright/test'

test('saved season still loads competitions and sends selected championship to overview', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('football_stats_competition_context_v1', JSON.stringify({ seasonId: '1', competitionId: '2' })))
  const requests = []
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const url = new URL(route.request().url())
    requests.push(url.pathname + url.search)
    let body = {}
    if (url.pathname === '/api/seasons') body = [{ id: 1, name: 'Сезон 2026', status: 'ACTIVE', roundsCount: 2, regularToursCount: 18 }]
    if (url.pathname === '/api/seasons/1/competitions') body = [{ id: 2, seasonId: 1, type: 'CHAMPIONSHIP', name: 'Чемпионат' }]
    if (url.pathname === '/api/seasons/1/overview') body = { teams: [], tours: [], standings: [] }
    if (url.pathname.endsWith('/player-stats')) body = []
    if (url.pathname.includes('notifications')) body = { items: [], count: 0, totalPages: 0 }
    await route.fulfill({ json: body })
  })
  await page.goto('/')
  await expect(page.locator('.competition-context-picker select').nth(1)).toHaveValue('2')
  await expect(page.locator('.season-meta-grid')).toBeVisible()
  expect(requests).toContain('/api/seasons/1/competitions')
  expect(requests).toContain('/api/seasons/1/overview?competitionId=2')
  await page.reload()
  await expect(page.locator('.competition-context-picker select').nth(1)).toHaveValue('2')
  await expect(page.locator('.season-meta-grid')).toBeVisible()
})

test('without a competition season settings are not shown as tournament statistics', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('football_stats_competition_context_v1', JSON.stringify({ seasonId: '1', competitionId: '2' })))
  const requests = []
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const url = new URL(route.request().url())
    requests.push(url.pathname)
    let body = {}
    if (url.pathname === '/api/seasons') body = [{ id: 1, name: 'Сезон', status: 'ACTIVE', roundsCount: 2, regularToursCount: 18 }]
    if (url.pathname === '/api/seasons/1/competitions') body = []
    await route.fulfill({ json: body })
  })
  await page.goto('/')
  await expect(page.locator('.error-text')).toContainText('нет доступных соревнований')
  await expect(page.locator('.season-meta-grid')).toHaveCount(0)
  expect(requests).not.toContain('/api/seasons/1/overview')
})
