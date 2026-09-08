import { test, expect } from '@playwright/test'

test('player profile shows details, history and restores keyboard focus', async ({ page }, testInfo) => {
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    const player = { id: 1, fullName: 'Александр Сергеевич Белов', birthDate: '1990-01-01', currentTeamName: 'Атлетик Богородск', residence: 'Богородск', isGoalkeeper: true, goals: 0, yellowCards: 2, redCards: 0 }
    let json = []
    if (path === '/api/players') json = { content: [player], number: 0, totalElements: 1, totalPages: 1, last: true }
    if (path === '/api/players/1') json = player
    if (path === '/api/players/1/history') json = { history: [{ teamId: 1, teamName: 'Атлетик Богородск', validFrom: '2025-01-01', validTo: null, active: true }] }
    await route.fulfill({ json })
  })
  await page.goto('/players')
  const trigger = page.getByRole('button', { name: /Александр Сергеевич Белов/ })
  await trigger.click()
  const dialog = page.getByRole('dialog', { name: 'Александр Сергеевич Белов' })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByText('Атлетик Богородск', { exact: true })).toHaveCount(1)
  await expect(dialog.getByText('Фото не добавлено')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Закрыть карточку игрока' })).toBeFocused()
  await page.keyboard.press('Shift+Tab')
  await expect(dialog.getByRole('button', { name: /История команд/ })).toBeFocused()
  await page.keyboard.press('Enter')
  await expect(dialog.getByText('по настоящее время', { exact: false })).toBeVisible()
  const dimensions = await dialog.evaluate((element) => ({ width: element.clientWidth, scroll: element.scrollWidth }))
  expect(dimensions.scroll).toBeLessThanOrEqual(dimensions.width)
  await dialog.screenshot({ path: testInfo.outputPath('player-profile.png') })
  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden()
  await expect(trigger).toBeFocused()

  await page.goto('/players/1')
  const linkedDialog = page.getByRole('dialog', { name: 'Александр Сергеевич Белов' })
  await expect(linkedDialog).toBeVisible()
  await linkedDialog.getByRole('button', { name: 'Закрыть карточку игрока' }).click()
  await expect(page).toHaveURL(/\/players$/)

  await page.goto('/players/1?from=hall-of-fame')
  await expect(page.getByRole('button', { name: 'Назад в Зал славы' })).toBeVisible()
  await page.getByRole('button', { name: 'Назад в Зал славы' }).click()
  await expect(page).toHaveURL(/\/hall-of-fame$/)
})
