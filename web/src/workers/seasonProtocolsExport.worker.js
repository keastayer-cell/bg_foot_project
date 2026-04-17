import JSZip from 'jszip'
import { createMatchProtocolPdfBlob } from '../utils/matchProtocolPdf'

function normalizeProtocolStatValue(value) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < 0) return 0
  return Math.floor(parsed)
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function buildWorkerPdfData(seasonName, match) {
  return {
    seasonName,
    tourName: match.tourName || 'Матч сезона',
    kickoffLabel: formatDateTime(match.kickoffAt),
    statusLabel: 'Протокол подтвержден',
    scoreLabel: `${normalizeProtocolStatValue(match.homeScore)} : ${normalizeProtocolStatValue(match.awayScore)}`,
    homeTeamName: match.homeTeamName || 'Команда 1',
    awayTeamName: match.awayTeamName || 'Команда 2',
    referees: Array.isArray(match.referees) ? match.referees.map((referee, index) => ({
      key: `referee-${index + 1}`,
      label: referee.label,
      name: referee.name || 'Не назначен',
    })) : [],
    teams: Array.isArray(match.teams) ? match.teams.map((team) => ({
      teamName: team.teamName,
      players: Array.isArray(team.players) ? team.players.map((player) => ({
        playerId: player.playerId,
        playerName: player.playerName,
        sortOrder: normalizeProtocolStatValue(player.sortOrder),
        goals: normalizeProtocolStatValue(player.goals),
        yellowCards: normalizeProtocolStatValue(player.yellowCards),
        redCards: normalizeProtocolStatValue(player.redCards),
      })) : [],
    })) : [],
    note: match.note || 'Дополнительные замечания по матчу не указаны.',
    fileName: match.fileName || 'match-protocol.pdf',
  }
}

self.onmessage = async (event) => {
  const payload = event?.data || {}
  if (payload.type !== 'generateSeasonProtocolsArchive') {
    return
  }

  try {
    const seasonName = String(payload.seasonName || 'season')
    const matches = Array.isArray(payload.matches) ? payload.matches : []
    const archiveName = String(payload.archiveName || `Протоколы_${seasonName}.zip`)

    if (!matches.length) {
      throw new Error('Нет подтвержденных протоколов для выгрузки.')
    }

    const zip = new JSZip()

    for (let index = 0; index < matches.length; index += 1) {
      self.postMessage({
        type: 'progress',
        text: `Протоколы ${index + 1}/${matches.length}`,
      })

      const pdfBlob = await createMatchProtocolPdfBlob(buildWorkerPdfData(seasonName, matches[index]))
      zip.file(matches[index].fileName || `protocol_${index + 1}.pdf`, pdfBlob, { compression: 'STORE' })
    }

    self.postMessage({ type: 'progress', text: 'Упаковка архива...' })

    const archiveBytes = await zip.generateAsync(
      { type: 'uint8array', compression: 'STORE' },
      (metadata) => {
        const percent = Math.max(0, Math.min(100, Math.round(Number(metadata?.percent || 0))))
        self.postMessage({ type: 'progress', text: `Упаковка архива ${percent}%` })
      }
    )

    self.postMessage(
      {
        type: 'done',
        archiveName,
        buffer: archiveBytes.buffer,
      },
      [archiveBytes.buffer]
    )
  } catch (error) {
    self.postMessage({
      type: 'error',
      error: error instanceof Error ? error.message : 'Не удалось сформировать архив протоколов.',
    })
  }
}