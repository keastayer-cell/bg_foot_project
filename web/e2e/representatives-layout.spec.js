import { test, expect } from '@playwright/test'
test.use({ viewport: { width: 1280, height: 900 } })
test('representative assignment has aligned controls and clear current team', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('football_stats_persistent_session', '1'))
  const rep = { id: 2, userId: 2, name: 'Представитель Волны', email: 'rep@example.com', roles: ['TEAM_REP'], teamScopes: [{ teamId: 1, teamName: 'Волна Дуденево', canEditRoster: true, canEditApplication: true }] }
  await page.route('http://127.0.0.1:8080/api/**', async route => {
    const url = new URL(route.request().url()); let json = []
    if (url.pathname.startsWith('/api/auth/') || url.pathname === '/api/admin/access/me') json = { token: 'test', id: 1, userId: 1, name: 'Антон', roles: ['SUPER_ADMIN'], teamScopes: [] }
    else if (url.pathname === '/api/admin/access/users') json = { content: [rep], totalPages: 1 }
    else if (url.pathname === '/api/admin/access/users/2') json = rep
    else if (url.pathname === '/api/teams') json = [{ id: 1, name: 'Волна Дуденево' }, { id: 2, name: 'Атлетик Богородск' }]
    else if (url.pathname.includes('notifications')) json = { items: [], count: 0 }
    await route.fulfill({ json })
  })
  await page.goto('/admin')
  await page.getByRole('button', { name: 'Представители', exact: true }).click()
  const panel = page.locator('.representatives-panel')
  await panel.locator('.representative-search-grid select').selectOption('rep@example.com')
  await panel.getByRole('button', { name: 'Открыть', exact: true }).click()
  await expect(panel.locator('.representative-current-team strong')).toHaveText('Волна Дуденево')
  await page.mouse.move(0, 0)
  await expect.poll(async () => {
    const field = await panel.locator('.representative-search-grid input').boundingBox()
    const button = await panel.locator('.representative-search-grid button').boundingBox()
    return Math.abs(field.y - button.y)
  }).toBeLessThan(1)
  const boxes = await Promise.all(['input', 'select', 'button'].map(selector => panel.locator(`.representative-search-grid ${selector}`).boundingBox()))
  for (const box of boxes) { expect(box.height).toBe(46); expect(Math.abs(box.y - boxes[0].y)).toBeLessThan(1) }
  await page.screenshot({ path: '/tmp/bg-representatives-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: '/tmp/bg-representatives-mobile.png', fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})
