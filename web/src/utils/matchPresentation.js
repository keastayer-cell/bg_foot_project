export function stripTeamSuffix(playerName, ...teamNames) {
  const normalizedName = String(playerName || '').trim()

  for (const teamName of teamNames) {
    const normalizedTeamName = String(teamName || '').trim()
    if (!normalizedTeamName) continue

    const suffix = ` (${normalizedTeamName})`
    if (normalizedName.endsWith(suffix)) {
      return normalizedName.slice(0, -suffix.length).trim()
    }
  }

  return normalizedName
}
