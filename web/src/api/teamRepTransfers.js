export function createTeamRepTransfersApi(request) {
  return {
    getSeasons(privileged) {
      return request(privileged ? '/api/seasons?active_flag=0' : '/api/team-rep/seasons', {
        method: 'GET',
      })
    },
    getOverview(seasonId, pageNum, pageSize) {
      return request(
        `/api/team-rep/seasons/${encodeURIComponent(seasonId)}/transfers?pagenum=${pageNum}&pagesize=${pageSize}`,
        { method: 'GET' },
      )
    },
    getCandidates(seasonId, fromTeamId, toTeamId) {
      return request(
        `/api/team-rep/seasons/${encodeURIComponent(seasonId)}/transfer-candidates/${encodeURIComponent(fromTeamId)}?toTeamId=${encodeURIComponent(toTeamId)}`,
        { method: 'GET' },
      )
    },
    create(seasonId, payload) {
      return request(`/api/team-rep/seasons/${encodeURIComponent(seasonId)}/transfers`, {
        method: 'POST',
        body: JSON.stringify(payload),
      })
    },
    process(requestId, action, decisionComment = null) {
      return request(
        `/api/team-rep/transfers/${encodeURIComponent(requestId)}/${encodeURIComponent(action)}`,
        {
          method: 'POST',
          body: JSON.stringify({ decisionComment }),
        },
      )
    },
  }
}
