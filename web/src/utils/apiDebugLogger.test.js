import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { installApiDebugLogger } from './apiDebugLogger'

function createResponse() {
  return {
    status: 200,
    ok: true,
    headers: {
      get: () => 'application/json',
    },
    clone: () => ({
      json: async () => ({ status: 'ok' }),
      text: async () => '',
    }),
  }
}

describe('installApiDebugLogger', () => {
  let originalFetch

  beforeEach(() => {
    originalFetch = vi.fn().mockResolvedValue(createResponse())
    globalThis.window = {
      location: { origin: 'https://football.test' },
      fetch: originalFetch,
    }

    vi.spyOn(console, 'groupCollapsed').mockImplementation(() => {})
    vi.spyOn(console, 'log').mockImplementation(() => {})
    vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.spyOn(console, 'groupEnd').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
    delete globalThis.window
  })

  it('does not patch fetch when logging is disabled', () => {
    installApiDebugLogger({
      apiBaseUrl: 'https://api.football.test',
      enabled: false,
    })

    expect(window.fetch).toBe(originalFetch)
  })

  it('installs only once', () => {
    installApiDebugLogger({
      apiBaseUrl: 'https://api.football.test',
      enabled: true,
    })
    const installedFetch = window.fetch

    installApiDebugLogger({
      apiBaseUrl: 'https://api.football.test',
      enabled: true,
    })

    expect(window.fetch).toBe(installedFetch)
  })

  it('passes non-API requests through without logging', async () => {
    installApiDebugLogger({
      apiBaseUrl: 'https://api.football.test',
      enabled: true,
    })

    await window.fetch('https://cdn.football.test/logo.png')

    expect(originalFetch).toHaveBeenCalledWith('https://cdn.football.test/logo.png', {})
    expect(console.groupCollapsed).not.toHaveBeenCalled()
  })

  it('logs API requests and masks sensitive request fields', async () => {
    installApiDebugLogger({
      apiBaseUrl: 'https://api.football.test',
      enabled: true,
    })

    await window.fetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        email: 'user@example.com',
        password: 'secret-value',
      }),
    })

    expect(console.groupCollapsed).toHaveBeenCalledWith('[API] POST /api/auth/login')
    expect(console.log).toHaveBeenCalledWith(
      'Запрос:',
      expect.objectContaining({
        body: {
          email: 'user@example.com',
          password: '***',
        },
      }),
    )
  })
})
