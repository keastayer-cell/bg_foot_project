export function createAdminLeagueApi(request) {
  const resource = (name) => ({
    list: () => request(`/api/admin/league/${name}?active_flag=1`, { method: 'GET' }),
    create: (payload) => request(`/api/admin/league/${name}`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
    update: (id, payload) => request(`/api/admin/league/${name}/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
    deactivate: (id) => request(`/api/admin/league/${name}/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    }),
  })

  return {
    officials: resource('officials'),
    venues: resource('venues'),
    regulations: {
      list() {
        return request('/api/admin/league/regulations', { method: 'GET' })
      },
      save(targetType, targetId, documentDataUrl) {
        const resourceName = targetType === 'SEASON' ? 'seasons' : 'competitions'
        return request(`/api/admin/league/${resourceName}/${encodeURIComponent(targetId)}/regulation`, {
          method: 'PUT',
          body: JSON.stringify({ documentDataUrl }),
        })
      },
      remove(targetType, targetId) {
        const resourceName = targetType === 'SEASON' ? 'seasons' : 'competitions'
        return request(`/api/admin/league/${resourceName}/${encodeURIComponent(targetId)}/regulation`, {
          method: 'DELETE',
        })
      },
    },
  }
}
