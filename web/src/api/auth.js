import { requestJson } from './http'

export function completePasswordReset(apiBaseUrl, token, newPassword) {
  return requestJson(apiBaseUrl, '/api/auth/password-reset/complete', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  })
}
