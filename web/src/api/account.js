export function createAccountApi(authorizedRequest) {
  return {
    get() {
      return authorizedRequest('/api/account', { method: 'GET' })
    },
    update({ email, name }) {
      return authorizedRequest('/api/account', {
        method: 'PATCH',
        body: JSON.stringify({ email, name }),
      })
    },
    changePassword({ currentPassword, newPassword }) {
      return authorizedRequest('/api/account/password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword }),
      })
    },
    logoutAll() {
      return authorizedRequest('/api/account/logout-all', { method: 'POST' })
    },
    getNotificationSettings() {
      return authorizedRequest('/api/account/notification-settings', { method: 'GET' })
    },
    updateNotificationSettings(settings) {
      return authorizedRequest('/api/account/notification-settings', {
        method: 'PUT',
        body: JSON.stringify(settings),
      })
    },
    getFavorites() {
      return authorizedRequest('/api/account/favorites', { method: 'GET' })
    },
    addFavorite(type, targetId) {
      return authorizedRequest(`/api/account/favorites/${encodeURIComponent(type)}/${encodeURIComponent(targetId)}`, { method: 'POST' })
    },
    removeFavorite(type, targetId) {
      return authorizedRequest(`/api/account/favorites/${encodeURIComponent(type)}/${encodeURIComponent(targetId)}`, { method: 'DELETE' })
    },
  }
}
