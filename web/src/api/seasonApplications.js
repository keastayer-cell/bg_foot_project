export function createSeasonApplicationsApi(request) {
  return {
    getSeasons() {
      return request('/api/seasons?active_flag=0', { method: 'GET' })
    },
    getQueue(seasonId) {
      const query = seasonId ? `?seasonId=${encodeURIComponent(seasonId)}` : ''
      return request(`/api/season-applications${query}`, { method: 'GET' })
    },
    getDetails(applicationId) {
      return request(`/api/season-applications/${encodeURIComponent(applicationId)}`, {
        method: 'GET',
      })
    },
    process(applicationId, action, decisionComment) {
      return request(
        `/api/season-applications/${encodeURIComponent(applicationId)}/${encodeURIComponent(action)}`,
        {
          method: 'POST',
          body: JSON.stringify({ decisionComment: decisionComment || null }),
        },
      )
    },
  }
}
