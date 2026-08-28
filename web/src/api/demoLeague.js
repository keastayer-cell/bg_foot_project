export function createDemoLeagueApi(request) {
  return {
    status() {
      return request('/api/admin/demo/league')
    },
    createBase() {
      return request('/api/admin/demo/league', { method: 'POST' })
    },
    createSchedule() {
      return request('/api/admin/demo/league/schedule', { method: 'POST' })
    },
    addResults() {
      return request('/api/admin/demo/league/results', { method: 'POST' })
    },
    prepareTransfers() {
      return request('/api/admin/demo/league/transfers', { method: 'POST' })
    },
    preparePlayoffs() {
      return request('/api/admin/demo/league/playoffs', { method: 'POST' })
    },
    reset() {
      return request('/api/admin/demo/league', { method: 'DELETE' })
    },
  }
}
