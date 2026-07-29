import { expect, test } from '@playwright/test'

const admin = {
  userId: 1,
  id: 1,
  email: 'admin@example.com',
  name: 'Admin',
  roles: ['SUPER_ADMIN'],
  mustChangePassword: false,
}

async function restoreAdminSession(page) {
  await page.route('http://127.0.0.1:8080/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname

    if (path === '/api/auth/refresh') {
      await route.fulfill({ json: { token: 'test-token', ...admin } })
      return
    }
    if (path === '/api/auth/me') {
      await route.fulfill({ json: admin })
      return
    }
    if (path === '/api/admin/access/me') {
      await route.fulfill({ json: { roles: admin.roles, teamScopes: [] } })
      return
    }
    if (path === '/api/health') {
      await route.fulfill({ json: { status: 'UP' } })
      return
    }

    await route.fulfill({ json: [] })
  })

  await page.addInitScript(() => {
    localStorage.setItem('football_stats_persistent_session', '1')
  })
}

test.beforeEach(async ({ page }) => {
  await restoreAdminSession(page)
})

test('shows every backend endpoint with a visible test action', async ({ page }) => {
  await page.goto('/api-explorer')

  await expect(page.getByRole('heading', { name: 'API Explorer' })).toBeVisible()
  await expect(page.locator('.api-endpoint-row')).toHaveCount(97)
  await expect(page.locator('.api-test-link')).toHaveCount(97)
})

test('filters endpoints and executes a GET request', async ({ page }) => {
  await page.goto('/api-explorer')
  await page.getByLabel('Поиск').fill('/api/health')

  await expect(page.locator('.api-endpoint-row')).toHaveCount(2)
  await page.locator('a[href="/api-explorer/test/health-main"]').click()

  await expect(page).toHaveURL(/\/api-explorer\/test\/health-main$/)
  await page.getByRole('button', { name: 'Отправить запрос' }).click()
  await expect(page.getByText('HTTP 200')).toBeVisible()
  await expect(page.locator('.api-response-output')).toContainText('"status": "UP"')
})

test('requires confirmation for requests that change data', async ({ page }) => {
  await page.goto('/api-explorer/test/auth-logout')

  const submit = page.getByRole('button', { name: 'Отправить запрос' })
  await expect(submit).toBeDisabled()
  await page.getByLabel('Подтверждаю выполнение запроса, изменяющего данные').check()
  await expect(submit).toBeEnabled()
})

test('has no horizontal overflow', async ({ page }) => {
  await page.goto('/api-explorer')

  const layout = await page.evaluate(() => ({
    viewportWidth: window.innerWidth,
    documentWidth: document.documentElement.scrollWidth,
  }))

  expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewportWidth)
})

test('keeps the endpoint test screen layout intact', async ({ page }, testInfo) => {
  await page.goto('/api-explorer/test/auth-me')
  await expect(page.getByRole('heading', { name: 'Текущий пользователь' })).toBeVisible()

  const layout = await page.evaluate(() => {
    const head = document.querySelector('.api-test-head')
    const request = document.querySelector('.api-request-layout')
    return {
      headDisplay: getComputedStyle(head).display,
      requestDisplay: getComputedStyle(request).display,
      documentWidth: document.documentElement.scrollWidth,
      viewportWidth: window.innerWidth,
    }
  })

  expect(layout.headDisplay).toBe('grid')
  expect(layout.requestDisplay).toBe('grid')
  expect(layout.documentWidth).toBeLessThanOrEqual(layout.viewportWidth)

  const breadcrumb = await page.locator('.api-test-breadcrumb').boundingBox()
  const title = await page.locator('.api-test-title-block').boundingBox()
  const access = await page.locator('.api-test-access').boundingBox()
  const builder = await page.locator('.api-request-builder').boundingBox()
  const preview = await page.locator('.api-request-preview').boundingBox()

  expect(breadcrumb).not.toBeNull()
  expect(title).not.toBeNull()
  expect(access).not.toBeNull()
  expect(builder).not.toBeNull()
  expect(preview).not.toBeNull()

  if (testInfo.project.metadata.mobile) {
    expect(access.y).toBeGreaterThanOrEqual(title.y + title.height)
    expect(builder.y).toBeGreaterThanOrEqual(preview.y + preview.height)
  } else {
    expect(access.x).toBeGreaterThanOrEqual(title.x + title.width)
    expect(preview.x).toBeGreaterThanOrEqual(builder.x + builder.width)
  }
})
