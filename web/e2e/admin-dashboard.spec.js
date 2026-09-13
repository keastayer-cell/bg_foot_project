import { test, expect } from '@playwright/test'
for (const legacy of [false, true]) test(`dashboard renders scoped tasks, legacy=${legacy}`, async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('football_stats_persistent_session', '1'))
  const calls = []
  await page.route('http://127.0.0.1:8080/api/**', async route => {
    const url = new URL(route.request().url())
    let json = []
    if (url.pathname.startsWith('/api/auth/') || url.pathname === '/api/admin/access/me') json = { token: 'test', id: 1, userId: 1, name: 'Антон', roles: ['SUPER_ADMIN'], teamScopes: [] }
    else if (url.pathname === '/api/seasons') json = [{ id: 1, name: '2026' }, { id: 2, name: '2027' }, { id: 3, name: 'Пустой сезон' }]
    else if (url.pathname.endsWith('/competitions')) json = url.pathname.includes('/1/') ? [{ id: 10, name: 'Чемпионат', type: 'CHAMPIONSHIP' }, { id: 11, name: 'Кубок', type: 'CUP' }] : url.pathname.includes('/2/') ? [{ id: 20, name: 'Зимний кубок', type: 'CUP' }] : []
    else if (url.pathname === '/api/admin/dashboard') {
      const season = url.searchParams.get('seasonId'), competition = url.searchParams.get('competitionId')
      calls.push([season, competition])
      json = [{ key: 'suspensions', label: 'Дисквалификации', count: Number(competition), path: `/discipline?season=${season}&competition=${competition}`, ...(legacy ? {} : { scope: 'COMPETITION' }) }]
    } else if (url.pathname.includes('notifications')) json = { items: [], count: 0 }
    await route.fulfill({ json })
  })
  await page.goto('/admin?season=1&competition=11')
  const panel = page.locator('.dashboard-panel')
  if (legacy) { await expect(panel).toContainText('Backend вернул сводку старого формата'); return }
  await expect(panel.getByRole('link', { name: /Дисквалификации/ })).toHaveAttribute('href', '/discipline?season=1&competition=11')
  await panel.locator('.competition-context-picker select').nth(1).selectOption('10')
  await expect(panel.getByRole('link', { name: /Дисквалификации/ })).toHaveAttribute('href', '/discipline?season=1&competition=10')
  await panel.locator('.competition-context-picker select').nth(0).selectOption('2')
  await expect(panel.getByRole('link', { name: /Дисквалификации/ })).toHaveAttribute('href', '/discipline?season=2&competition=20')
  await panel.locator('.competition-context-picker select').nth(0).selectOption('3')
  await expect(panel).toContainText('Выберите соревнование')
  expect(calls).toEqual([['1','11'], ['1','10'], ['2','20']])
})
