import { test, expect } from '@playwright/test'
test.use({ viewport: { width: 1280, height: 900 } })
test('roles keeps other users available after opening a card', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('football_stats_persistent_session', '1'))
  const rep = { id: 2, userId: 2, name: 'Представитель Волны', email: 'rep@example.com', roles: ['TEAM_REP'], teamScopes: [{ teamId: 1, teamName: 'Волна Дуденево', canEditRoster: true, canEditApplication: true }] }
  await page.route('http://127.0.0.1:8080/api/**', async route => {
    const url = new URL(route.request().url()); let json = []
    if (url.pathname.startsWith('/api/auth/') || url.pathname === '/api/admin/access/me') json = { token: 'test', id: 1, userId: 1, name: 'Антон', roles: ['SUPER_ADMIN'], teamScopes: [] }
    else if (url.pathname === '/api/admin/access/users') json = { content: url.searchParams.get('email') ? [rep] : [rep, { ...rep, id: 3, userId: 3, name: 'Представитель Атлетика', email: 'other@example.com' }], totalPages: 1 }
    else if (url.pathname === '/api/admin/access/users/2') json = rep
    else if (url.pathname === '/api/teams') json = [{ id: 1, name: 'Волна Дуденево' }, { id: 2, name: 'Атлетик Богородск' }]
    else if (url.pathname.includes('notifications')) json = { items: [], count: 0 }
    await route.fulfill({ json })
  })
  await page.goto('/admin')
  await page.getByRole('button', { name: 'Роли и доступ', exact: true }).click()
  const panel = page.locator('.access-panel')
  const select = panel.locator('.access-search-grid select')
  await select.selectOption('rep@example.com')
  await panel.getByRole('button', { name: 'Открыть', exact: true }).click()
  await expect(panel.locator('.access-user-name')).toHaveText('Представитель Волны')
  await expect(select.locator('option')).toHaveCount(3)
  await select.selectOption('other@example.com')
  await panel.getByRole('button', { name: 'Открыть', exact: true }).click()
  await expect(panel.locator('.access-user-name')).toHaveText('Представитель Атлетика')
  await expect(select.locator('option')).toHaveCount(3)
  await page.setViewportSize({ width: 1920, height: 1080 })
  await page.screenshot({ path: '/tmp/bg-roles-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: '/tmp/bg-roles-mobile.png', fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})
