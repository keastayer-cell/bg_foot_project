<template>
  <article class="card admin-tabs-wrap">
    <div class="admin-tab-groups">
      <section v-for="group in groups" :key="group.id" class="admin-tab-group">
        <div class="admin-tab-group-head">
          <span class="admin-tab-group-kicker">{{ group.kicker }}</span>
          <h3 class="admin-tab-group-title">{{ group.title }}</h3>
        </div>
        <div class="admin-tabs admin-tabs-grid">
          <button
            v-for="tab in group.items"
            :key="tab.id"
            class="btn-ghost admin-tab-btn"
            :class="{ 'admin-tab-active': activeTab === tab.id }"
            type="button"
            @click="$emit('select', tab.id)"
          >
            <span class="admin-tab-label">{{ tab.label }}</span>
          </button>
        </div>
      </section>
    </div>
  </article>
</template>

<script setup>
defineProps({
  groups: {
    type: Array,
    required: true,
  },
  activeTab: {
    type: String,
    required: true,
  },
})

defineEmits(['select'])
</script>

<style scoped>
.admin-tab-groups {
  align-items: start;
}

.admin-tab-group,
.admin-tabs-grid {
  align-content: start;
}

@media (max-width: 640px) {
  .admin-tab-group {
    padding: 14px;
  }
}
</style>
