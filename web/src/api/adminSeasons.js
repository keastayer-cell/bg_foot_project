export function createAdminSeasonsApi(request, rawRequest) {
  return {
    async create(payload, teamIds) {
      const season = await request('/api/seasons', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      await this.setTeams(season.id, teamIds)
      return season
    },
    update(seasonId, payload) {
      return request(`/api/seasons/${encodeURIComponent(seasonId)}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      })
    },
    setTeams(seasonId, teamIds) {
      return request(`/api/seasons/${encodeURIComponent(seasonId)}/teams`, {
        method: 'PUT',
        body: JSON.stringify({ teamIds }),
      })
    },
    completeRegularSeason(seasonId) {
      return request(`/api/seasons/${encodeURIComponent(seasonId)}/complete-regular-season`, {
        method: 'POST',
      })
    },
    deactivate(seasonId) {
      return request(`/api/seasons/${encodeURIComponent(seasonId)}`, { method: 'DELETE' })
    },
    getActive() {
      return request('/api/seasons?active_flag=1', { method: 'GET' })
    },
    getTeams(seasonId) {
      return request(`/api/seasons/${encodeURIComponent(seasonId)}/teams`, { method: 'GET' })
    },
    exportProtocols(seasonId) {
      return rawRequest(`/api/seasons/${encodeURIComponent(seasonId)}/protocols/export`, {
        method: 'GET',
      })
    },
  }
}
