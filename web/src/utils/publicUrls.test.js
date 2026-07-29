import { describe, expect, it } from 'vitest'

import { matchPageLocation, publicSlug, teamProfileLocation } from './publicUrls'

describe('publicUrls', () => {
  it('builds readable ASCII slugs from Russian names', () => {
    expect(publicSlug('Искра Окский')).toBe('iskra-okskiy')
    expect(publicSlug('Факел — Лакша')).toBe('fakel-laksha')
  })

  it('keeps route identifiers out of the visible team URL', () => {
    expect(teamProfileLocation(
      { id: 16, name: 'Искра Окский' },
      { seasonId: 1 },
    )).toEqual({
      name: 'team-profile',
      params: { slug: 'iskra-okskiy' },
      state: { teamId: 16, teamSlug: 'iskra-okskiy', seasonId: '1' },
    })
  })

  it('keeps match return context out of the visible URL', () => {
    expect(matchPageLocation(92, {
      returnContext: 'team-profile',
      teamSlug: 'meteor-novinki',
    })).toEqual({
      name: 'match',
      params: { id: '92' },
      state: {
        returnContext: 'team-profile',
        teamSlug: 'meteor-novinki',
      },
    })
  })
})
