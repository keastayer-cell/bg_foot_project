export function createCatalogApi(request) {
  return {
    getSeasons(activeFlag = 1) {
      return request(`/api/seasons?active_flag=${activeFlag}`, { method: 'GET' })
    },
    getSeasonOverview(seasonId) {
      return request(`/api/seasons/${encodeURIComponent(seasonId)}/overview`, { method: 'GET' })
    },
    getSeasonPlayerStats(seasonId) {
      return request(`/api/seasons/${encodeURIComponent(seasonId)}/player-stats`, { method: 'GET' })
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
