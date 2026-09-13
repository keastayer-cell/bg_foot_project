import { test, expect } from '@playwright/test'
test.use({ viewport: { width: 1280, height: 900 } })
test('transfer history prioritizes player, route and status', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('football_stats_persistent_session', '1'))
  await page.route('http://127.0.0.1:8080/api/**', async route => {
    const url = new URL(route.request().url()); let json = []
    if (url.pathname.startsWith('/api/auth/') || url.pathname === '/api/admin/access/me') json = { token: 'test', id: 1, userId: 1, name: 'Антон', roles: ['SUPER_ADMIN'], teamScopes: [] }
    else if (url.pathname === '/api/seasons') json = [{ id: 1, name: 'Демо-лига Богородского округа 2026' }]
    else if (url.pathname.endsWith('/transfers')) json = { seasonName: 'Демо-лига', seasonStatus: 'ACTIVE', transferWindowOpen: false, privilegedAccess: true, transferWindowStartDate: '2026-07-26', transferWindowEndDate: '2026-08-28', totalElements: 1, totalPages: 1, pageNumber: 0, sourceTeams: [], targetTeams: [], requests: [{ id: 1, playerName: 'Артём Волков (Атлетик)', fromTeamName: 'Атлетик Богородск', toTeamName: 'Волна Дуденево', requestedAt: '2026-07-29T10:00:00Z', requestedByName: 'Представитель Волны', requestComment: 'Проверка согласования перехода между командами.', status: 'REVOKED' }] }
    else if (url.pathname.includes('notifications')) json = { items: [], count: 0 }
    await route.fulfill({ json })
  })
  await page.goto('/team-rep-transfers')
  const assertAlignment = async () => {
    const boxes = await Promise.all(['.transfer-season-field select', '.transfer-window-state', '.transfer-context-layout>.btn-primary'].map(selector => page.locator(selector).boundingBox()))
    for (const box of boxes) { expect(box.height).toBe(56); expect(Math.abs(box.y - boxes[0].y)).toBeLessThan(1) }
  }
  await expect(page.locator('.transfer-window-state strong')).toHaveText('Выберите сезон')
  await assertAlignment()
  await page.locator('.transfer-season-field select').selectOption('1')
  await expect(page.getByText('Окно закрыто', { exact: true })).toBeVisible()
  await assertAlignment()
  await expect(page.getByRole('button', { name: 'Создать трансфер', exact: true })).toBeDisabled()
  await expect(page.getByText('Артём Волков (Атлетик)', { exact: true })).toBeVisible()
  await expect(page.getByText('Проверка согласования перехода между командами.', { exact: true })).toBeHidden()
  await page.locator('.transfer-request-details summary').click()
  await expect(page.getByText('Проверка согласования перехода между командами.', { exact: true })).toBeVisible()
  await page.locator('.transfer-request-details summary').click()
  await page.screenshot({ path: '/tmp/bg-transfers-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: '/tmp/bg-transfers-mobile.png', fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})
