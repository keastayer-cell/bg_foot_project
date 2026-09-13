import { test, expect } from '@playwright/test'
test('verified schedule is locked and server errors are visible in dialog', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('football_stats_persistent_session', '1'))
  await page.route('http://127.0.0.1:8080/api/**', async route => {
    const url = new URL(route.request().url()); let json = []
    if (url.pathname.startsWith('/api/auth/') || url.pathname === '/api/admin/access/me') json = { token: 'test', id: 1, userId: 1, name: 'Антон', roles: ['SUPER_ADMIN'], teamScopes: [] }
    else if (url.pathname === '/api/seasons') json = [{ id: 1, name: '2026', roundsCount: 2 }]
    else if (url.pathname.endsWith('/competitions')) json = [{ id: 1, name: 'Чемпионат', type: 'CHAMPIONSHIP' }]
    else if (url.pathname === '/api/tours') json = [{ id: 4, name: 'Тур 1', seasonId: 1, competitionId: 1, published: true }]
    else if (url.pathname === '/api/tours/4/matches') json = [{ id: 9, homeTeamName: 'Метеор Новинки', awayTeamName: 'Искра Окский', kickoffAt: '2026-09-13T10:00:00Z', protocolStatus: 'VERIFIED', scheduleStatus: 'COMPLETED' }, { id: 10, homeTeamName: 'Атлетик', awayTeamName: 'Волна', kickoffAt: '2026-09-14T10:00:00Z', protocolStatus: 'DRAFT', scheduleStatus: 'SCHEDULED' }]
    else if (url.pathname.endsWith('/schedule')) { await route.fulfill({ status: 400, json: { error: 'Расписание подтверждённого матча менять нельзя. Сначала откройте протокол.' } }); return }
    else if (url.pathname.includes('notifications')) json = { items: [], count: 0 }
    await route.fulfill({ json })
  })
  await page.goto('/admin')
  await page.getByRole('button', { name: 'Туры и матчи', exact: true }).click()
  await page.locator('.tour-context-fields select').first().selectOption('1')
  await page.locator('select').filter({ has: page.locator('option[value="4"]') }).selectOption('4')
  const locked = page.locator('.tour-match-item').filter({ hasText: 'Метеор Новинки' })
  await expect(locked.getByRole('button', { name: 'Расписание', exact: true })).toBeDisabled()
  await expect(locked.getByRole('button', { name: 'Удалить', exact: true })).toBeDisabled()
  await expect(locked.getByRole('button', { name: 'Удалить', exact: true })).toHaveCSS('cursor', 'not-allowed')
  await expect(locked).toContainText('Открыть протокол может только суперадминистратор')
  const editable = page.locator('.tour-match-item').filter({ hasText: 'Атлетик' })
  await editable.getByRole('button', { name: 'Расписание', exact: true }).click()
  const editor = await editable.locator('.tour-schedule-editor').boundingBox()
  for (const field of await editable.locator('.tour-schedule-editor input, .tour-schedule-editor select').all()) {
    const box = await field.boundingBox()
    expect(box.x + box.width).toBeLessThanOrEqual(editor.x + editor.width)
  }
  await page.screenshot({ path: '/tmp/bg-schedule-editor.png', fullPage: true })
  await editable.getByRole('button', { name: 'Сохранить', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  await expect(dialog).toContainText('Сначала откройте протокол')
  await dialog.getByRole('button', { name: 'Понятно' }).click()
  await expect(dialog).toBeHidden()
  await expect(editable.locator('.tour-schedule-editor')).toBeVisible()
})
