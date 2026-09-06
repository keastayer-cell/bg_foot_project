export function createNotificationsApi(request) {
  return {
    list(pageNumber = 0, pageSize = 20) {
      return request(`/api/notifications?pagenum=${pageNumber}&pagesize=${pageSize}`, { method: 'GET' })
    },
    unreadCount() {
      return request('/api/notifications/unread-count', { method: 'GET' })
    },
    publicList(pageNumber = 0, pageSize = 20) {
      return request(`/api/notifications/public?pagenum=${pageNumber}&pagesize=${pageSize}`, { method: 'GET' })
    },
    markRead(recipientId) {
      return request(`/api/notifications/${encodeURIComponent(recipientId)}/read`, { method: 'POST' })
    },
    acknowledge(recipientId) {
      return request(`/api/notifications/${encodeURIComponent(recipientId)}/acknowledge`, { method: 'POST' })
    },
    markAllRead() {
      return request('/api/notifications/read-all', { method: 'POST' })
    },
    adminList(pageNumber = 0, pageSize = 20) {
      return request(`/api/notifications/admin?pagenum=${pageNumber}&pagesize=${pageSize}`, { method: 'GET' })
    },
    create(payload) {
      return request('/api/notifications/admin', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
    },
  }
}
