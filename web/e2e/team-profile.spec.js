import { test, expect } from '@playwright/test'

test('team profile filters matches and opens the selected season roster', async ({ page }, testInfo) => {
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    let json = []
    if (path === '/api/teams/1') json = {
      id: 1, name: 'Атлетик Богородск', shortName: 'Атлетик', city: 'Богородск', active: true,
      seasons: [{ id: 1, name: 'Чемпионат 2026' }, { id: 2, name: 'Чемпионат 2025' }],
      recentMatches: Array.from({ length: 7 }, (_, i) => ({ matchId: i + 1, seasonId: i < 5 ? 1 : 2, seasonName: i < 5 ? 'Чемпионат 2026' : 'Чемпионат 2025', kickoffAt: '2026-09-06T12:00:00Z', opponentName: i < 5 ? 'Богородский машиностроитель' : 'Олимп', teamScore: 2, opponentScore: i % 2 ? 3 : 0, home: i % 2 === 0, resultCode: i % 2 ? 'L' : 'W', resultLabel: i % 2 ? 'Поражение' : 'Победа' })),
    }
    if (path.endsWith('/roster')) json = { status: 'APPROVED', representativeName: 'Алексей Петров', players: [{ id: 1, fullName: 'Александр Сергеевич Белов', birthDate: '1990-01-01', residence: 'Богородск' }] }
    await route.fulfill({ json })
  })
  await page.goto('/teams/1')
  await expect(page.getByRole('heading', { name: 'Атлетик Богородск' })).toBeVisible()
  const rows = page.locator('.team-profile-history-row')
  await expect(rows).toHaveCount(5)
  await page.locator('.team-profile-page').screenshot({ path: testInfo.outputPath('team-profile.png') })
  await page.getByRole('button', { name: 'Вперёд', exact: true }).click()
  await expect(rows).toHaveCount(2)
  await page.getByRole('combobox', { name: 'Сезон', exact: true }).selectOption('2')
  await expect(rows).toHaveCount(2)
  await expect(rows.first()).toContainText('Олимп')
  await expect(page.locator('.team-profile-pagination')).toBeHidden()
  await expect(page.locator('.team-profile-stat-card').first().locator('strong')).toHaveText('2')
  await page.getByRole('button', { name: 'Показать заявку' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText('Александр Сергеевич Белов')).toBeVisible()
  const dims = await page.evaluate(() => ({ viewport: innerWidth, content: document.documentElement.scrollWidth }))
  expect(dims.content).toBeLessThanOrEqual(dims.viewport)
  const modalDims = await dialog.evaluate((element) => ({ width: element.clientWidth, content: element.scrollWidth }))
  expect(modalDims.content).toBeLessThanOrEqual(modalDims.width)
  await dialog.screenshot({ path: testInfo.outputPath('team-roster.png') })
  await dialog.getByRole('button', { name: 'Закрыть' }).click()
  await expect(dialog).toBeHidden()

  await page.goto('/teams/1?from=hall-of-fame')
  await expect(page.getByRole('link', { name: 'Назад в Зал славы' })).toHaveAttribute('href', '/hall-of-fame')
})
