export function createMatchesApi({
  optionalRequest,
  optionalRawRequest,
  authorizedRequest,
}) {
  return {
    getMatch(matchId) {
      return optionalRequest(`/api/matches/${encodeURIComponent(matchId)}`, { method: 'GET' })
    },

    downloadProtocol(matchId) {
      return optionalRawRequest(`/api/matches/${encodeURIComponent(matchId)}/protocol/pdf`, { method: 'GET' })
    },

    saveLineup(matchId, teamId, playerIds) {
      return authorizedRequest(
        `/api/matches/${encodeURIComponent(matchId)}/lineups/${encodeURIComponent(teamId)}`,
        {
          method: 'PUT',
          body: JSON.stringify({ playerIds }),
        },
      )
    },

    saveProtocol(matchId, payload) {
      return authorizedRequest(`/api/matches/${encodeURIComponent(matchId)}/protocol`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      })
    },

    reopenProtocol(matchId) {
      return authorizedRequest(`/api/matches/${encodeURIComponent(matchId)}/protocol/reopen`, {
        method: 'POST',
      })
    },
  }
}
