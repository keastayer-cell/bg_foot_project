<template>
  <aside class="admin-navigation" aria-label="Разделы админ-панели">
    <div class="admin-navigation-mobile">
      <label for="admin-section-select">Раздел</label>
      <select id="admin-section-select" :value="activeTab" @change="selectFromMenu">
        <optgroup v-for="group in groups" :key="group.id" :label="group.title">
          <option v-for="tab in group.items" :key="tab.id" :value="tab.id">
            {{ tab.label }}{{ tab.external ? ' ↗' : '' }}
          </option>
        </optgroup>
      </select>
    </div>

    <div class="admin-navigation-desktop">
      <section v-for="group in groups" :key="group.id" class="admin-navigation-group">
        <h3 class="admin-navigation-title">{{ group.title }}</h3>
        <div class="admin-navigation-items">
          <button
            v-for="tab in group.items"
            :key="tab.id"
            class="admin-navigation-button"
            :class="{ 'is-active': activeTab === tab.id }"
            type="button"
            :aria-current="activeTab === tab.id ? 'page' : undefined"
            :title="tab.external ? 'Открыть в новой вкладке' : undefined"
            @click="$emit('select', tab.id)"
          >
            <span>{{ tab.label }}</span>
            <span v-if="tab.external" class="admin-navigation-external" aria-hidden="true">↗</span>
          </button>
        </div>
      </section>
    </div>
  </aside>
</template>

<script setup>
const props = defineProps({
  groups: {
    type: Array,
    required: true,
  },
  activeTab: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['select'])

function selectFromMenu(event) {
  const tabId = event.target.value
  const selectedTab = props.groups
    .flatMap((group) => group.items)
    .find((tab) => tab.id === tabId)

  emit('select', tabId)

  if (selectedTab?.external) {
    event.target.value = props.activeTab
  }
}
</script>

<style scoped>
.admin-navigation {
  position: sticky;
  top: 14px;
  align-self: start;
  display: grid;
  min-width: 0;
  border: 1px solid rgba(124, 163, 255, 0.16);
  border-radius: 8px;
  background: rgba(10, 17, 38, 0.86);
  overflow: hidden;
}

.admin-navigation-mobile {
  display: none;
}

.admin-navigation-desktop {
  display: grid;
}

.admin-navigation-group {
  display: grid;
  gap: 6px;
  padding: 14px 10px;
  border-bottom: 1px solid rgba(124, 163, 255, 0.12);
}

.admin-navigation-group:last-child {
  border-bottom: 0;
}

.admin-navigation-title {
  margin: 0;
  padding: 0 8px;
  color: rgba(208, 214, 235, 0.58);
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
}

.admin-navigation-items {
  display: grid;
  gap: 3px;
}

.admin-navigation-button {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  width: 100%;
  padding: 9px 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: rgba(239, 243, 255, 0.78);
  font: inherit;
  font-weight: 650;
  text-align: left;
  cursor: pointer;
}

.admin-navigation-button:hover {
  border-color: rgba(124, 163, 255, 0.18);
  background: rgba(31, 47, 66, 0.58);
  color: var(--text);
}

.admin-navigation-button:focus-visible {
  outline: 2px solid rgba(97, 232, 162, 0.72);
  outline-offset: -2px;
}

.admin-navigation-button.is-active {
  border-color: rgba(97, 232, 162, 0.34);
  background: rgba(31, 76, 66, 0.5);
  color: var(--text);
}

.admin-navigation-external {
  color: rgba(151, 176, 255, 0.78);
  font-size: 0.9rem;
}

@media (max-width: 860px) {
  .admin-navigation {
    position: static;
  }

  .admin-navigation-desktop {
    display: none;
  }

  .admin-navigation-mobile {
    display: grid;
    gap: 6px;
    padding: 12px;
    color: rgba(208, 214, 235, 0.68);
    font-size: 0.78rem;
    font-weight: 700;
  }

  .admin-navigation-mobile select {
    width: 100%;
    min-height: 46px;
    margin: 0;
  }
}
</style>
