import { describe, expect, it, vi } from 'vitest'
import { createNotificationsApi } from './notifications'

describe('notifications api', () => {
  it('loads public notifications without a user recipient', async () => {
    const request = vi.fn(async () => ({}))
    await createNotificationsApi(request).publicList(0, 10)
    expect(request).toHaveBeenCalledWith('/api/notifications/public?pagenum=0&pagesize=10', { method: 'GET' })
  })

  it('marks a recipient notification as read', async () => {
    const request = vi.fn(async () => ({}))
    await createNotificationsApi(request).markRead(17)
    expect(request).toHaveBeenCalledWith('/api/notifications/17/read', { method: 'POST' })
  })

  it('creates an administrative notification', async () => {
    const request = vi.fn(async () => ({}))
    const payload = { title: 'Старт сезона', audienceType: 'ALL' }
    await createNotificationsApi(request).create(payload)
    expect(request).toHaveBeenCalledWith('/api/notifications/admin', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })
})
