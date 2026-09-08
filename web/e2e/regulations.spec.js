import { expect, test } from '@playwright/test'

const targets = [
  { targetType: 'SEASON', targetId: 1, seasonId: 1, seasonName: 'Сезон 2026', seasonStatus: 'ACTIVE', competitionName: null, competitionType: null, available: true, updatedAt: '2026-09-01T10:00:00Z', downloadUrl: '/api/seasons/1/regulation/pdf' },
  { targetType: 'COMPETITION', targetId: 11, seasonId: 1, seasonName: 'Сезон 2026', seasonStatus: 'ACTIVE', competitionName: 'Чемпионат города', competitionType: 'CHAMPIONSHIP', available: false, updatedAt: null, downloadUrl: null },
  { targetType: 'COMPETITION', targetId: 12, seasonId: 1, seasonName: 'Сезон 2026', seasonStatus: 'ACTIVE', competitionName: 'Кубок открытия', competitionType: 'CUP', available: true, updatedAt: '2026-09-02T10:00:00Z', downloadUrl: '/api/competitions/12/regulation/pdf' },
]

test('public regulations clearly identify season, championship and cup', async ({ page }) => {
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/league/overview') {
      await route.fulfill({ json: { officials: [], venues: [], seasonDocuments: [] } })
      return
    }
    if (path === '/api/league/regulations') {
      await route.fulfill({ json: targets.filter((item) => item.available) })
      return
    }
    await route.fulfill({ json: [] })
  })

  await page.goto('/league')
  await page.getByRole('button', { name: 'Регламенты' }).click()
  await expect(page.getByRole('heading', { name: 'Сезон 2026' })).toBeVisible()
  await expect(page.getByText('Общий регламент сезона', { exact: true })).toBeVisible()
  await expect(page.getByText('Кубок открытия', { exact: true })).toBeVisible()
  await expect(page.getByText('Кубок', { exact: true })).toBeVisible()
  await expect(page.locator('.regulation-document')).toHaveCount(2)

  const layout = await page.evaluate(() => ({ viewport: innerWidth, content: document.documentElement.scrollWidth }))
  expect(layout.content).toBeLessThanOrEqual(layout.viewport)
})

test('admin uploads a PDF only to the selected competition', async ({ page }) => {
  let savedRequest = null
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    if (path === '/api/auth/refresh') {
      await route.fulfill({ json: { token: 'test-token', userId: 1, email: 'admin@example.com', name: 'Admin', roles: ['SUPER_ADMIN'], mustChangePassword: false } })
      return
    }
    if (path === '/api/auth/me') {
      await route.fulfill({ json: { id: 1, userId: 1, email: 'admin@example.com', name: 'Admin', roles: ['SUPER_ADMIN'], mustChangePassword: false } })
      return
    }
    if (path === '/api/admin/access/me') {
      await route.fulfill({ json: { roles: ['SUPER_ADMIN'], teamScopes: [] } })
      return
    }
    if (path === '/api/admin/league/regulations') {
      await route.fulfill({ json: targets })
      return
    }
    if (path === '/api/admin/league/competitions/11/regulation' && request.method() === 'PUT') {
      savedRequest = request.postDataJSON()
      await route.fulfill({ json: { ...targets[1], available: true } })
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
  await page.getByRole('button', { name: /Документы/ }).click()
  await page.getByRole('combobox', { name: 'К чему относится документ' }).selectOption('COMPETITION:11')
  await page.locator('input[type="file"]').setInputFiles({
    name: 'championship.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.4 test'),
  })
  await page.getByRole('button', { name: 'Опубликовать PDF' }).click()

  await expect.poll(() => savedRequest).not.toBeNull()
  expect(savedRequest.documentDataUrl).toMatch(/^data:application\/pdf;base64,/)
})
