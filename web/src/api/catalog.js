export function createCatalogApi(request) {
  return {
    search(query, limit = 5) {
      const params = new URLSearchParams({ q: query, limit: String(limit) })
      return request(`/api/search?${params}`, { method: 'GET' })
    },
    getCalendar(filters = {}) {
      const params = new URLSearchParams()
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== '' && value !== null && value !== undefined) params.set(key, value)
      })
      return request(`/api/calendar${params.size ? `?${params}` : ''}`, { method: 'GET' })
    },
    getDiscipline(competitionId) {
      return request(`/api/discipline?competitionId=${encodeURIComponent(competitionId)}`, { method: 'GET' })
    },
    adjustDiscipline(payload) {
      return request('/api/admin/discipline/adjustments', { method: 'POST', body: JSON.stringify(payload) })
    },
    getSeasons(activeFlag = 1) {
      return request(`/api/seasons?active_flag=${activeFlag}`, { method: 'GET' })
    },
    getSeasonOverview(seasonId, competitionId) {
      return request(`/api/seasons/${encodeURIComponent(seasonId)}/overview${competitionId ? `?competitionId=${encodeURIComponent(competitionId)}` : ''}`, { method: 'GET' })
    },
    getSeasonPlayerStats(seasonId, competitionId) {
      return request(`/api/seasons/${encodeURIComponent(seasonId)}/player-stats${competitionId ? `?competitionId=${encodeURIComponent(competitionId)}` : ''}`, { method: 'GET' })
    },
    getSeasonTransfers(seasonId, pageNum, pageSize) {
      return request(
        `/api/seasons/${encodeURIComponent(seasonId)}/transfers?pagenum=${pageNum}&pagesize=${pageSize}`,
        { method: 'GET' },
      )
    },
    getPlayers(params) {
      return request(`/api/players?${params.toString()}`)
    },
    getPlayer(playerId) {
      return request(`/api/players/${encodeURIComponent(playerId)}`)
    },
    getPlayerHistory(playerId) {
      return request(`/api/players/${encodeURIComponent(playerId)}/history`)
    },
    getLeagueOverview() {
      return request('/api/league/overview', { method: 'GET' })
    },
    getLeagueRegulations() {
      return request('/api/league/regulations', { method: 'GET' })
    },
    getHallOfFame() {
      return request('/api/hall-of-fame', { method: 'GET' })
    },
    getActiveTeams() {
      return request('/api/teams?active_flag=1')
    },
    getTeam(teamId) {
      return request(`/api/teams/${encodeURIComponent(teamId)}`, { method: 'GET' })
    },
    getTeamSeasonRoster(teamId, seasonId) {
      return request(
        `/api/teams/${encodeURIComponent(teamId)}/seasons/${encodeURIComponent(seasonId)}/roster`,
        { method: 'GET' },
      )
    },
  }
}
