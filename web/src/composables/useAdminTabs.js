import { computed, ref } from 'vue'

export const ADMIN_TAB_GROUPS = [
  {
    id: 'competition',
    kicker: 'Турнир',
    title: 'Соревнование и участники',
    items: [
      { id: 'seasons', label: 'Сезоны' },
      { id: 'season-applications', label: 'Заявки на сезон' },
      { id: 'transfers', label: 'Трансферы' },
      { id: 'teams', label: 'Команды' },
      { id: 'players', label: 'Игроки' },
      { id: 'referees', label: 'Судьи' },
      { id: 'tours', label: 'Туры и матчи' },
    ],
  },
  {
    id: 'access',
    kicker: 'Доступ',
    title: 'Права и модерация',
    items: [
      { id: 'roles', label: 'Роли и доступ' },
      { id: 'league', label: 'Лига' },
      { id: 'representatives', label: 'Представители' },
      { id: 'ban', label: 'Блокировки' },
    ],
  },
]

const EXTERNAL_TABS = {
  'season-applications': '/season-applications-review',
  transfers: '/team-rep-transfers',
}

export function useAdminTabs({ hasRole, navigate }) {
  const activeTab = ref('seasons')

  const visibleTabGroups = computed(() => {
    if (hasRole('SUPER_ADMIN')) {
      return ADMIN_TAB_GROUPS
    }
    if (hasRole('REFEREE')) {
      return ADMIN_TAB_GROUPS.filter((group) => group.id !== 'access')
    }
    return []
  })

  function selectAdminTab(tabId) {
    const targetPath = EXTERNAL_TABS[tabId]
    if (targetPath) {
      navigate(targetPath)
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
