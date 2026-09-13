import { test, expect } from '@playwright/test'
test.use({ viewport: { width: 1280, height: 900 } })
test('profile sections are readable and preserve form input', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('football_stats_persistent_session', '1'))
  await page.route('http://127.0.0.1:8080/api/**', async route => {
    const url = new URL(route.request().url())
    let json = []
    if (url.pathname.startsWith('/api/auth/') || url.pathname === '/api/admin/access/me' || url.pathname === '/api/account') json = { token: 'test', id: 1, userId: 1, name: 'Антон', email: 'admin@example.com', roles: ['SUPER_ADMIN'], roleAnnotations: [{ code: 'SUPER_ADMIN', userTitle: 'Супер администратор', description: 'Полный доступ к управлению лигой.' }], teamScopes: [{ teamId: 1, teamName: 'Искра Окский', canEditRoster: true, canEditApplication: true }] }
    else if (url.pathname.endsWith('notification-settings')) json = ['MATCHES','TOURNAMENTS','TRANSFERS','DISCIPLINE','NEWS'].map(category => ({ category, bellEnabled: true, emailEnabled: true }))
    else if (url.pathname.includes('notifications')) json = { items: [], count: 0 }
    await route.fulfill({ json })
  })
  await page.goto('/profile')
  await expect(page.locator('.account-role-chip')).toHaveText('Супер администратор')
  await page.locator('#account-profile input').first().fill('Новое имя')
  await page.getByRole('button', { name: /Уведомления Колокольчик/ }).click()
  await expect(page.getByRole('checkbox', { name: 'Матчи: email', exact: true })).toBeVisible()
  await expect(page.locator('#account-profile')).toBeHidden()
  await page.screenshot({ path: '/tmp/bg-profile-notifications.png', fullPage: true })
  await page.getByRole('button', { name: /Безопасность Пароль/ }).click()
  await expect(page.getByRole('button', { name: 'Выйти на всех устройствах' })).toBeVisible()
  await page.getByRole('button', { name: /Мои данные Имя/ }).click()
  await expect(page.locator('#account-profile input').first()).toHaveValue('Новое имя')
  await page.screenshot({ path: '/tmp/bg-profile-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: '/tmp/bg-profile-mobile.png', fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})
