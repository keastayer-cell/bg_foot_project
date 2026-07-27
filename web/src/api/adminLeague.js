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
    saveRegulation(seasonId, documentDataUrl) {
      return request(
        `/api/admin/league/seasons/${encodeURIComponent(seasonId)}/regulation`,
        {
          method: 'PUT',
          body: JSON.stringify({ documentDataUrl }),
        },
      )
    },
    removeRegulation(seasonId) {
      return request(
        `/api/admin/league/seasons/${encodeURIComponent(seasonId)}/regulation`,
        { method: 'DELETE' },
      )
    },
  }
}
