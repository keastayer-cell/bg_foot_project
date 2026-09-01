export function createCompetitionsApi(request) {
  const base = (seasonId) => `/api/seasons/${encodeURIComponent(seasonId)}/competitions`
  return {
    list: (seasonId) => request(base(seasonId), { method: 'GET' }),
    createCup: (seasonId, payload) => request(`${base(seasonId)}/cups`, { method: 'POST', body: JSON.stringify(payload) }),
    createChampionship: (seasonId, name) => request(`${base(seasonId)}/championships`, { method: 'POST', body: JSON.stringify({ name }) }),
    renameChampionship: (seasonId, competitionId, name) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}/championship`, { method: 'PUT', body: JSON.stringify({ name }) }),
    update: (seasonId, competitionId, payload) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}`, { method: 'PUT', body: JSON.stringify(payload) }),
    deactivate: (seasonId, competitionId) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}`, { method: 'DELETE' }),
    draw: (seasonId, competitionId, orderedTeamIds = []) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}/draw`, {
      method: 'POST',
      body: JSON.stringify({ orderedTeamIds }),
    }),
    confirmDraw: (seasonId, competitionId) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}/draw/confirm`, { method: 'POST' }),
    scheduleTie: (seasonId, competitionId, tieId, kickoffDates) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}/ties/${encodeURIComponent(tieId)}/matches`, {
      method: 'POST',
      body: JSON.stringify({ kickoffDates }),
    }),
    chooseTieWinner: (seasonId, competitionId, tieId, homePenaltyScore, awayPenaltyScore) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}/ties/${encodeURIComponent(tieId)}/winner`, {
      method: 'POST', body: JSON.stringify({ homePenaltyScore, awayPenaltyScore }),
    }),
    roster: (seasonId, competitionId) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}/roster`, { method: 'GET' }),
    rosterCandidates: (seasonId, competitionId, teamId) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}/roster/candidates?teamId=${encodeURIComponent(teamId)}`, { method: 'GET' }),
    addRosterPlayers: (seasonId, competitionId, teamId, playerIds) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}/roster`, {
      method: 'POST', body: JSON.stringify({ teamId, playerIds }),
    }),
    removeRosterPlayer: (seasonId, competitionId, teamId, playerId) => request(`${base(seasonId)}/${encodeURIComponent(competitionId)}/roster/${encodeURIComponent(playerId)}?teamId=${encodeURIComponent(teamId)}`, { method: 'DELETE' }),
  }
}
