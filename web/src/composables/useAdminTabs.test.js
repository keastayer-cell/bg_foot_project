import { describe, expect, it, vi } from 'vitest'

import { useAdminTabs } from './useAdminTabs'

function createTabs(roles = []) {
  return useAdminTabs({
    hasRole: (role) => roles.includes(role),
    openExternal: vi.fn(),
    demoToolsEnabled: true,
  })
}

describe('useAdminTabs', () => {
  it('shows all groups to a super admin', () => {
    const { visibleTabGroups } = createTabs(['SUPER_ADMIN'])

    expect(visibleTabGroups.value.map((group) => group.id)).toEqual([
      'competition',
      'participants',
      'access',
      'local-tools',
    ])
    expect(visibleTabGroups.value.flatMap((group) => group.items).map((item) => item.id))
      .toContain('demo-league')
  })

  it('shows competition and participant tools to a referee', () => {
    const { visibleTabGroups } = createTabs(['REFEREE'])

    expect(visibleTabGroups.value.map((group) => group.id)).toEqual([
      'competition',
      'participants',
    ])
    expect(visibleTabGroups.value.flatMap((group) => group.items).map((item) => item.id))
      .toContain('league')
  })

  it('keeps access management exclusive to a super admin', () => {
    const { visibleTabGroups } = createTabs(['REFEREE'])
    const tabIds = visibleTabGroups.value.flatMap((group) => group.items).map((item) => item.id)

    expect(tabIds).not.toContain('roles')
    expect(tabIds).not.toContain('representatives')
    expect(tabIds).not.toContain('ban')
  })

  it('does not expose admin groups without an accepted role', () => {
    const { visibleTabGroups } = createTabs(['USER'])

    expect(visibleTabGroups.value).toEqual([])
  })

  it.each([
    ['season-applications', '/season-applications-review'],
    ['transfers', '/team-rep-transfers'],
  ])('opens the %s workflow externally', (tabId, path) => {
    const openExternal = vi.fn()
    const { activeTab, selectAdminTab } = useAdminTabs({
      hasRole: () => true,
      openExternal,
    })

    selectAdminTab(tabId)

    expect(openExternal).toHaveBeenCalledWith(path)
    expect(activeTab.value).toBe('seasons')
  })

  it('marks dedicated workflows as external navigation items', () => {
    const { visibleTabGroups } = createTabs(['SUPER_ADMIN'])
    const externalIds = visibleTabGroups.value
      .flatMap((group) => group.items)
      .filter((item) => item.external)
      .map((item) => item.id)

    expect(externalIds).toEqual(['season-applications', 'transfers'])
  })

  it('changes the active inline tab', () => {
    const { activeTab, selectAdminTab } = createTabs(['SUPER_ADMIN'])

    selectAdminTab('teams')

    expect(activeTab.value).toBe('teams')
  })
})
