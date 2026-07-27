export async function requestJson(apiBaseUrl, path, options = {}) {
  const response = await requestRaw(apiBaseUrl, path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) {
    const error = new Error(body.error || 'Не удалось выполнить запрос.')
    error.status = response.status
    error.body = body
    throw error
  }
  return body
}

export async function requestRaw(apiBaseUrl, path, options = {}) {
  try {
    return await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      credentials: options.credentials || 'include',
      headers: {
        ...(options.headers || {}),
      },
    })
  } catch {
    throw new Error('Сервер недоступен. Попробуйте позже.')
  }
}
