let pdfConfigured = false
let pdfMakeInstance = null

async function ensurePdfConfigured() {
  if (pdfConfigured && pdfMakeInstance) return pdfMakeInstance

  const [pdfMakeModule, pdfFontsModule] = await Promise.all([
    import('pdfmake/build/pdfmake'),
    import('pdfmake/build/vfs_fonts'),
  ])

  pdfMakeInstance = pdfMakeModule.default || pdfMakeModule
  const vfs = pdfFontsModule.default || pdfFontsModule

  if (vfs && typeof pdfMakeInstance.addVirtualFileSystem === 'function') {
    pdfMakeInstance.addVirtualFileSystem(vfs)
  }

  pdfConfigured = true
  return pdfMakeInstance
}

function getRefereeName(referees, label) {
  return referees.find((referee) => referee.label === label)?.name || 'Не назначен'
}

function buildTeamTable(team) {
  return {
    margin: [0, 0, 0, 12],
    table: {
      headerRows: 2,
      widths: [28, '*', 42, 42, 42],
      body: [
        [
          { text: team.teamName, colSpan: 5, style: 'teamHeader' },
          {},
          {},
          {},
          {},
        ],
        [
          { text: '№', style: 'tableHeader' },
          { text: 'Игрок', style: 'tableHeader' },
          { text: 'Г', style: 'tableHeader' },
          { text: 'ЖК', style: 'tableHeader' },
          { text: 'КК', style: 'tableHeader' },
        ],
        ...(team.players?.length
          ? team.players.map((player) => ([
              { text: String(player.sortOrder ?? ''), alignment: 'center' },
              { text: player.playerName || '', alignment: 'left' },
              { text: player.goals ? String(player.goals) : '—', alignment: 'center' },
              { text: player.yellowCards ? String(player.yellowCards) : '—', alignment: 'center' },
              { text: player.redCards ? String(player.redCards) : '—', alignment: 'center' },
            ]))
          : [[
              { text: 'Заявка команды пока не заполнена.', colSpan: 5, color: '#6b7280', margin: [0, 6, 0, 6] },
              {},
              {},
              {},
              {},
            ]]),
      ],
    },
    layout: {
      fillColor: (rowIndex) => {
        if (rowIndex === 0) return '#e8edf7'
        if (rowIndex === 1) return '#f4f7fb'
        return null
      },
      hLineColor: () => '#cfd7e6',
      vLineColor: () => '#cfd7e6',
      paddingLeft: () => 8,
      paddingRight: () => 8,
      paddingTop: () => 6,
      paddingBottom: () => 6,
    },
  }
}

function buildMatchProtocolDocDefinition(data) {
  const chiefReferee = getRefereeName(data.referees, 'Главный арбитр')
  const assistantRefereeOne = getRefereeName(data.referees, 'Помощник 1')
  const assistantRefereeTwo = getRefereeName(data.referees, 'Помощник 2')

  return {
    pageSize: 'A4',
    pageMargins: [28, 24, 28, 18],
    info: {
      title: `Протокол матча ${data.homeTeamName} - ${data.awayTeamName}`,
      author: 'Футбол Богородск',
      subject: 'Протокол матча',
    },
    defaultStyle: {
      font: 'Roboto',
      fontSize: 10,
      color: '#182033',
    },
    content: [
      { text: 'Протокол матча', style: 'title' },
      { text: `${data.seasonName} · ${data.tourName}`, style: 'subtitle' },
      {
        stack: [
          { text: `Дата: ${data.kickoffLabel}`, margin: [0, 0, 0, 3] },
          { text: `Статус: ${data.statusLabel}`, margin: [0, 0, 0, 10] },
          {
            text: `${data.homeTeamName}   ${data.scoreLabel}   ${data.awayTeamName}`,
            style: 'scoreLine',
          },
        ],
        margin: [0, 0, 0, 14],
      },
      { text: 'Составы команд', style: 'sectionTitle' },
      ...data.teams.map((team) => buildTeamTable(team)),
      {
        stack: [
          { text: `Главный судья: ${chiefReferee}` },
          { text: `Помощник 1: ${assistantRefereeOne}`, margin: [0, 4, 0, 0] },
          { text: `Помощник 2: ${assistantRefereeTwo}`, margin: [0, 4, 0, 0] },
        ],
        margin: [0, 2, 0, 14],
      },
      { text: 'Примечание', style: 'sectionTitle' },
      {
        table: {
          widths: ['*'],
          body: [[{ text: data.note || 'Дополнительные замечания по матчу не указаны.', margin: [0, 10, 0, 54] }]],
        },
        layout: {
          hLineColor: () => '#cfd7e6',
          vLineColor: () => '#cfd7e6',
        },
        margin: [0, 0, 0, 12],
      },
      {
        table: {
          widths: [150, '*'],
          body: [
            ['Главный судья', '____________________________'],
          ],
        },
        layout: 'noBorders',
        margin: [0, 0, 0, 0],
      },
    ],
    styles: {
      title: {
        fontSize: 20,
        bold: true,
        margin: [0, 0, 0, 4],
      },
      subtitle: {
        fontSize: 11,
        color: '#5b6477',
        margin: [0, 0, 0, 12],
      },
      scoreLine: {
        fontSize: 18,
        bold: true,
        alignment: 'center',
        margin: [0, 4, 0, 0],
      },
      sectionTitle: {
        fontSize: 12,
        bold: true,
        margin: [0, 0, 0, 8],
      },
      teamHeader: {
        bold: true,
        margin: [0, 2, 0, 2],
      },
      tableHeader: {
        bold: true,
        alignment: 'center',
      },
    },
  }
}

function sanitizeFileName(fileName, fallback = 'match-protocol') {
  return String(fileName || fallback).replace(/[\\/:*?"<>|]/g, '_')
}

export async function createMatchProtocolPdfBlob(data) {
  const pdfMake = await ensurePdfConfigured()
  const pdfDocument = pdfMake.createPdf(buildMatchProtocolDocDefinition(data))

  if (typeof pdfDocument.getBlob === 'function') {
    return await new Promise((resolve, reject) => {
      pdfDocument.getBlob((blob) => {
        if (blob) {
          resolve(blob)
          return
        }
        reject(new Error('Не удалось сформировать PDF протокола.'))
      })
    })
  }

  if (typeof pdfDocument.getBuffer === 'function') {
    return await new Promise((resolve, reject) => {
      pdfDocument.getBuffer((buffer) => {
        if (buffer) {
          resolve(new Blob([buffer], { type: 'application/pdf' }))
          return
        }
        reject(new Error('Не удалось сформировать PDF протокола.'))
      })
    })
  }

  throw new Error('PDF генератор не поддерживает выгрузку файла.')
}

export async function downloadMatchProtocolPdf(data) {
  const blob = await createMatchProtocolPdfBlob(data)
  const filename = sanitizeFileName(data.fileName || 'match-protocol', 'match-protocol')
  const objectUrl = URL.createObjectURL(blob)

  try {
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = filename
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    link.remove()
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}