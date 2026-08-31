import { requestJson } from './http'

export function requestPasswordReset(apiBaseUrl, email) {
  return requestJson(apiBaseUrl, '/api/auth/password-reset/request', {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function completePasswordReset(apiBaseUrl, token, newPassword) {
  return requestJson(apiBaseUrl, '/api/auth/password-reset/complete', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  })
}
