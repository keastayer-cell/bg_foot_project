import { describe, expect, it, vi } from 'vitest'
import { createAccountApi } from './account'

describe('account api', () => {
  it('uses protected account endpoints', async () => {
    const request = vi.fn().mockResolvedValue({})
    const api = createAccountApi(request)

    await api.get()
    await api.update({ email: 'new@example.com', name: 'Новое имя' })
    await api.changePassword({ currentPassword: 'old-pass', newPassword: 'new-pass' })
    await api.logoutAll()
    await api.getNotificationSettings()
    await api.updateNotificationSettings([{ category: 'MATCHES', bellEnabled: true, emailEnabled: false }])

    expect(request).toHaveBeenNthCalledWith(1, '/api/account', { method: 'GET' })
    expect(request).toHaveBeenNthCalledWith(2, '/api/account', {
      method: 'PATCH',
      body: JSON.stringify({ email: 'new@example.com', name: 'Новое имя' }),
    })
    expect(request).toHaveBeenNthCalledWith(3, '/api/account/password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword: 'old-pass', newPassword: 'new-pass' }),
    })
    expect(request).toHaveBeenNthCalledWith(4, '/api/account/logout-all', { method: 'POST' })
    expect(request).toHaveBeenNthCalledWith(5, '/api/account/notification-settings', { method: 'GET' })
    expect(request).toHaveBeenNthCalledWith(6, '/api/account/notification-settings', {
      method: 'PUT',
      body: JSON.stringify([{ category: 'MATCHES', bellEnabled: true, emailEnabled: false }]),
    })
  })
})
