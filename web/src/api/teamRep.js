export function createTeamRepApi(authorizedRequest) {
  return {
    getActiveTeams() {
      return authorizedRequest('/api/teams?active_flag=1', { method: 'GET' })
    },

    getDashboard(scopedPath) {
      return Promise.all([
        authorizedRequest(scopedPath('/api/team-rep/seasons'), { method: 'GET' }),
        authorizedRequest(scopedPath('/api/team-rep/players'), { method: 'GET' }),
      ])
    },

    getIncomingTransfers(pageNum, pageSize = 20) {
      return authorizedRequest(
        `/api/team-rep/transfers/incoming-pending?pagenum=${pageNum}&pagesize=${pageSize}`,
        { method: 'GET' },
      )
    },

    processIncomingTransfer(requestId, action, decisionComment) {
      return authorizedRequest(
        `/api/team-rep/transfers/${encodeURIComponent(requestId)}/${encodeURIComponent(action)}`,
        {
          method: 'POST',
          body: JSON.stringify({ decisionComment: decisionComment || null }),
        },
      )
    },

    getSeasonPlayers(scopedPath, seasonId) {
      return authorizedRequest(
        scopedPath(`/api/team-rep/seasons/${encodeURIComponent(seasonId)}/players`),
        { method: 'GET' },
      )
    },

    addSeasonPlayers(scopedPath, seasonId, playerIds) {
      return authorizedRequest(
        scopedPath(`/api/team-rep/seasons/${encodeURIComponent(seasonId)}/players`),
        {
          method: 'POST',
          body: JSON.stringify({ playerIds }),
        },
      )
    },

    submitSeasonApplication(scopedPath, seasonId) {
      return authorizedRequest(
        scopedPath(`/api/team-rep/seasons/${encodeURIComponent(seasonId)}/submit`),
        { method: 'POST' },
      )
    },

    mutateSeasonPlayer(scopedPath, seasonId, playerId, method) {
      return authorizedRequest(
        scopedPath(
          `/api/team-rep/seasons/${encodeURIComponent(seasonId)}/players/${encodeURIComponent(playerId)}`,
        ),
        { method },
      )
    },

    removeTeamPlayer(teamId, playerId) {
      return authorizedRequest(
        `/api/teams/${encodeURIComponent(teamId)}/players/${encodeURIComponent(playerId)}`,
        { method: 'DELETE' },
      )
    },

    savePlayer(playerId, payload) {
      const path = playerId
        ? `/api/team-rep/players/${encodeURIComponent(playerId)}`
        : '/api/team-rep/players'
      return authorizedRequest(path, {
        method: playerId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      })
    },
  }
}
