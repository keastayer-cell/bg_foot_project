import { expect, test } from '@playwright/test'

const publicRoutes = ['/', '/league', '/transfers', '/players', '/teams']

async function mockPublicApi(page) {
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname

    if (path === '/api/players') {
      await route.fulfill({
        json: {
          content: [{ id: 1, fullName: 'Тестовый игрок', birthDate: '2000-01-01' }],
          number: 0,
          totalPages: 1,
          totalElements: 1,
          last: true,
        },
      })
      return
    }

    if (path === '/api/teams') {
      await route.fulfill({ json: [{ id: 1, name: 'Тестовая команда' }] })
      return
    }

    if (path === '/api/league/overview') {
      await route.fulfill({ json: { officials: [], venues: [], seasonDocuments: [] } })
      return
    }

    await route.fulfill({ json: [] })
  })
}

test.beforeEach(async ({ page }, testInfo) => {
  test.skip(!testInfo.project.metadata.mobile, 'Проверка предназначена для мобильных проектов')
  await mockPublicApi(page)
})

test('public pages use the device viewport without horizontal overflow', async ({ page }) => {
  for (const route of publicRoutes) {
    await page.goto(route)

    const layout = await page.evaluate(() => ({
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
    }))

    expect(layout.viewportWidth).toBeLessThanOrEqual(430)
    expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewportWidth)
  }
})

test('catalog filters stay compact on mobile', async ({ page }) => {
  for (const route of ['/players', '/teams']) {
    await page.goto(route)
    const input = page.locator('input[type="text"]')

    await expect(input).toBeVisible()
    expect((await input.boundingBox())?.height).toBeLessThanOrEqual(64)
  }
})

test('primary mobile navigation and player rows are touch friendly', async ({ page }) => {
  await page.goto('/players')

  const navLinks = page.locator('.topnav a')
  for (let index = 0; index < await navLinks.count(); index += 1) {
    expect((await navLinks.nth(index).boundingBox())?.height).toBeGreaterThanOrEqual(44)
  }

  const playerButton = page.getByRole('button', { name: 'Тестовый игрок' })
  await expect(playerButton).toBeVisible()
  expect((await playerButton.boundingBox())?.height).toBeGreaterThanOrEqual(44)
})
