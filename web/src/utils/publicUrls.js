const CYRILLIC_TRANSLITERATION = {
  а: 'a',
  б: 'b',
  в: 'v',
  г: 'g',
  д: 'd',
  е: 'e',
  ё: 'yo',
  ж: 'zh',
  з: 'z',
  и: 'i',
  й: 'y',
  к: 'k',
  л: 'l',
  м: 'm',
  н: 'n',
  о: 'o',
  п: 'p',
  р: 'r',
  с: 's',
  т: 't',
  у: 'u',
  ф: 'f',
  х: 'kh',
  ц: 'ts',
  ч: 'ch',
  ш: 'sh',
  щ: 'shch',
  ъ: '',
  ы: 'y',
  ь: '',
  э: 'e',
  ю: 'yu',
  я: 'ya',
}

export function publicSlug(value, fallback = 'item') {
  const source = String(value || '').trim().toLowerCase()
  const transliterated = [...source]
    .map((character) => CYRILLIC_TRANSLITERATION[character] ?? character)
    .join('')
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .replace(/-{2,}/g, '-')

  return transliterated || fallback
}

export function teamProfileLocation(team, options = {}) {
  const teamId = Number(team?.id)
  const seasonId = String(options.seasonId || '').trim()
  const teamSlug = publicSlug(team?.name, Number.isFinite(teamId) ? `team-${teamId}` : 'team')

  return {
    name: 'team-profile',
    params: {
      slug: teamSlug,
    },
    state: {
      ...(Number.isFinite(teamId) ? { teamId } : {}),
      teamSlug,
      ...(seasonId ? { seasonId } : {}),
    },
  }
}

export function matchPageLocation(matchId, returnState = {}) {
  return {
    name: 'match',
    params: {
      id: String(matchId),
    },
    state: {
      ...returnState,
    },
  }
}
