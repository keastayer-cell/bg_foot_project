import { describe, expect, it, vi } from 'vitest'

import { useAdminTabs } from './useAdminTabs'

function createTabs(roles = []) {
  return useAdminTabs({
    hasRole: (role) => roles.includes(role),
    navigate: vi.fn(),
  })
}

describe('useAdminTabs', () => {
  it('shows all groups to a super admin', () => {
    const { visibleTabGroups } = createTabs(['SUPER_ADMIN'])

    expect(visibleTabGroups.value.map((group) => group.id)).toEqual(['competition', 'access'])
  })

  it('shows only competition tools to a referee', () => {
    const { visibleTabGroups } = createTabs(['REFEREE'])

    expect(visibleTabGroups.value.map((group) => group.id)).toEqual(['competition'])
  })

  it('does not expose admin groups without an accepted role', () => {
    const { visibleTabGroups } = createTabs(['USER'])

    expect(visibleTabGroups.value).toEqual([])
  })

  it.each([
    ['season-applications', '/season-applications-review'],
    ['transfers', '/team-rep-transfers'],
  ])('navigates the %s tab to its dedicated page', (tabId, path) => {
    const navigate = vi.fn()
    const { activeTab, selectAdminTab } = useAdminTabs({
      hasRole: () => true,
      navigate,
    })

    selectAdminTab(tabId)

    expect(navigate).toHaveBeenCalledWith(path)
    expect(activeTab.value).toBe('seasons')
  })

  it('changes the active inline tab', () => {
    const { activeTab, selectAdminTab } = createTabs(['SUPER_ADMIN'])

    selectAdminTab('teams')

    expect(activeTab.value).toBe('teams')
  })
})
