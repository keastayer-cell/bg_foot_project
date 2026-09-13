import { test, expect } from '@playwright/test'

test('admin journal shows changes, filters and pagination', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('football_stats_persistent_session', '1'))
  const calls = []
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const url = new URL(route.request().url())
    const user = { token: 'test-token', id: 1, userId: 1, name: 'Антон', email: 'admin@example.com', roles: ['SUPER_ADMIN'], mustChangePassword: false, teamScopes: [] }
    if (url.pathname.startsWith('/api/auth/') || url.pathname === '/api/admin/access/me') { await route.fulfill({ json: user }); return }
    if (url.pathname === '/api/admin/audit') {
      calls.push(url.searchParams.toString())
      const secondPage = url.searchParams.get('pagenum') === '1'
      await route.fulfill({ json: { items: [{ id: secondPage ? 2 : 1, entityType: 'PROTOCOL', entityId: 10, action: 'PROTOCOL_VERIFIED', actorName: 'Антон', createdAt: '2026-09-13T10:00:00Z', path: '/matches/10', changes: { before: { record: [{ status: 'FINISHED' }] }, after: { record: [{ status: 'VERIFIED' }] } } }], totalElements: 26, totalPages: 2 } }); return
    }
    if (url.pathname.includes('notifications')) { await route.fulfill({ json: { items: [], totalPages: 0, count: 0 } }); return }
    await route.fulfill({ json: [] })
  })
  await page.goto('/admin')
  await page.getByRole('button', { name: 'Журнал действий', exact: true }).click()
  await expect(page.locator('.audit-entry').getByText('Подтверждён протокол', { exact: true })).toBeVisible()
  await page.getByText('Что изменилось', { exact: true }).click()
  await expect(page.locator('.audit-diff-wrap')).toContainText('FINISHED')
  await expect(page.locator('.audit-diff-wrap')).toContainText('VERIFIED')
  await expect(page.locator('.audit-entry').getByRole('link', { name: 'Открыть', exact: true })).toHaveAttribute('href', '/matches/10')
  await page.getByLabel('Автор', { exact: true }).fill('Антон')
  await page.getByRole('button', { name: 'Показать', exact: true }).click()
  await expect.poll(() => calls.some((call) => new URLSearchParams(call).get('author') === 'Антон')).toBe(true)
  await page.getByRole('button', { name: 'Вперёд', exact: true }).click()
  await expect(page.locator('.audit-pagination')).toContainText('2 / 2')
  expect(new URLSearchParams(calls.at(-1)).get('pagenum')).toBe('1')
})
