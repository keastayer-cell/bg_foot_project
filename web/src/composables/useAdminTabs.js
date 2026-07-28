import { computed, ref } from 'vue'

export const ADMIN_TAB_GROUPS = [
  {
    id: 'competition',
    title: 'Турнир',
    items: [
      { id: 'seasons', label: 'Сезоны' },
      { id: 'tours', label: 'Туры и матчи' },
      { id: 'season-applications', label: 'Заявки на сезон', external: true },
      { id: 'transfers', label: 'Трансферы', external: true },
      { id: 'league', label: 'Лига' },
    ],
  },
  {
    id: 'participants',
    title: 'Участники',
    items: [
      { id: 'teams', label: 'Команды' },
      { id: 'players', label: 'Игроки' },
      { id: 'referees', label: 'Судьи' },
    ],
  },
  {
    id: 'access',
    title: 'Доступ',
    roles: ['SUPER_ADMIN'],
    items: [
      { id: 'roles', label: 'Роли и доступ' },
      { id: 'representatives', label: 'Представители' },
      { id: 'ban', label: 'Блокировки' },
    ],
  },
]

const EXTERNAL_TABS = {
  'season-applications': '/season-applications-review',
  transfers: '/team-rep-transfers',
}

export function useAdminTabs({ hasRole, openExternal }) {
  const activeTab = ref('seasons')

  const visibleTabGroups = computed(() => {
    if (!hasRole('SUPER_ADMIN') && !hasRole('REFEREE')) return []

    return ADMIN_TAB_GROUPS
      .filter((group) => !group.roles || group.roles.some((role) => hasRole(role)))
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => !item.roles || item.roles.some((role) => hasRole(role))),
      }))
      .filter((group) => group.items.length)
  })

  function selectAdminTab(tabId) {
    const targetPath = EXTERNAL_TABS[tabId]
    if (targetPath) {
      openExternal(targetPath)
      return
    }
    activeTab.value = tabId
  }

  return {
    activeTab,
    visibleTabGroups,
    selectAdminTab,
  }
}
