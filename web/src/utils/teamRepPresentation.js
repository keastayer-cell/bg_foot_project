export function formatDateOnly(value) {
  return formatDate(value, false)
}

export function formatDateTime(value) {
  return formatDate(value, true)
}

export function formatPlayerOptionLabel(player) {
  return player?.fullName || ''
}

export function canEditApplicationSummary(summary) {
  if (!summary) return false
  const status = String(summary.applicationStatus || 'DRAFT')
  return Boolean(summary.applicationOpen)
    && ['DRAFT', 'RETURNED', 'APPROVED'].includes(status)
}

export function formatSeasonStatus(status) {
  return {
    ACTIVE: 'Активный',
    CLOSED: 'Закрыт',
    DRAFT: 'Черновик',
  }[status] || status || '—'
}

export function formatApplicationStatus(status) {
  return {
    DRAFT: 'Черновик',
    SUBMITTED: 'На проверке',
    RETURNED: 'На доработке',
    APPROVED: 'Одобрена',
    REJECTED: 'Отклонена',
  }[status] || status || '—'
}

export function applicationStatusChipClass(status) {
  if (status === 'APPROVED') return 'team-rep-season-chip-open'
  if (status === 'RETURNED' || status === 'REJECTED') return 'team-rep-season-chip-closed'
  if (status === 'SUBMITTED') return 'team-rep-season-chip-review'
  return ''
}

export function applicationReviewNoteClass(status) {
  return {
    APPROVED: 'team-rep-review-note-approved',
    RETURNED: 'team-rep-review-note-returned',
    REJECTED: 'team-rep-review-note-rejected',
  }[status] || ''
}

function formatDate(value, includeTime) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    ...(includeTime ? { hour: '2-digit', minute: '2-digit' } : {}),
  }).format(date)
}
