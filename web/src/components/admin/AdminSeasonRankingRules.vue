<template>
  <section class="admin-season-section admin-season-section-compact admin-season-rules-panel">
    <div class="admin-season-section-head admin-season-section-head-compact">
      <div>
        <h4 class="admin-list-title">Приоритет критериев</h4>
        <p class="muted-text">После очков критерии применяются сверху вниз.</p>
      </div>
      <button
        class="btn-ghost btn-sm"
        type="button"
        :disabled="form.rankingRules.length >= ruleOptions.length"
        @click="$emit('add')"
      >Добавить критерий</button>
    </div>
    <p v-if="!form.rankingRules.length" class="muted-text">Дополнительные критерии не выбраны.</p>
    <div v-else class="admin-ranking-rule-list">
      <div v-for="(rule, index) in form.rankingRules" :key="index" class="admin-ranking-rule-card">
        <span class="admin-ranking-rule-badge">{{ index + 1 }}</span>
        <label class="admin-ranking-rule-field">
          <span class="admin-ranking-rule-label">Приоритет {{ index + 1 }}</span>
          <select v-model="form.rankingRules[index]">
            <option value="">— выберите критерий —</option>
            <option
              v-for="option in availableOptions(index)"
              :key="`${index}-${option.value}`"
              :value="option.value"
            >{{ option.label }}</option>
          </select>
        </label>
        <div class="admin-ranking-rule-order">
          <button
            class="icon-button"
            type="button"
            title="Поднять критерий"
            :disabled="index === 0"
            @click="$emit('move', index, -1)"
          >↑</button>
          <button
            class="icon-button"
            type="button"
            title="Опустить критерий"
            :disabled="index === form.rankingRules.length - 1"
            @click="$emit('move', index, 1)"
          >↓</button>
        </div>
        <button class="admin-ranking-rule-remove" type="button" title="Убрать критерий" :aria-label="`Убрать приоритет ${index + 1}`" @click="$emit('remove', index)">×</button>
      </div>
    </div>
    <p class="admin-season-rules-summary"><span>Итоговый порядок</span>{{ summary() }}</p>
  </section>
</template>

<script setup>
defineProps({
  availableOptions: { type: Function, required: true },
  form: { type: Object, required: true },
  ruleOptions: { type: Array, required: true },
  summary: { type: Function, required: true },
})

defineEmits(['add', 'move', 'remove'])
</script>

<style scoped>
.admin-ranking-rule-card {
  grid-template-columns: auto minmax(0, 1fr) auto auto;
}

.admin-season-rules-panel {
  gap: 14px;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.admin-season-section-head {
  max-width: 900px;
}

.admin-ranking-rule-list {
  max-width: 900px;
}

.admin-ranking-rule-card {
  min-height: 58px;
  padding: 8px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.025);
}

.admin-ranking-rule-badge {
  width: 30px;
  height: 30px;
  border-radius: 50%;
}

.admin-ranking-rule-field {
  display: grid;
  grid-template-columns: 92px minmax(180px, 440px);
  align-items: center;
  gap: 10px;
}

.admin-ranking-rule-label {
  margin: 0;
}

.admin-ranking-rule-order {
  display: flex;
  gap: 6px;
  padding-bottom: 1px;
}

.admin-ranking-rule-order .icon-button {
  width: 34px;
  height: 34px;
}

.admin-ranking-rule-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid rgba(255, 104, 126, 0.32);
  border-radius: 6px;
  background: rgba(255, 104, 126, 0.08);
  color: #ff8fa1;
  font-size: 1.25rem;
  line-height: 1;
}

.admin-ranking-rule-remove:hover {
  border-color: rgba(255, 104, 126, 0.7);
  background: rgba(255, 104, 126, 0.16);
}

.admin-season-rules-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  max-width: 900px;
  margin: 0;
  padding-top: 2px;
  color: #dfe8ff;
}

.admin-season-rules-summary span {
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 700;
  text-transform: uppercase;
}

@media (max-width: 600px) {
  .admin-ranking-rule-card {
    grid-template-columns: auto minmax(0, 1fr);
    align-items: center;
  }

  .admin-ranking-rule-field {
    grid-column: 2;
    grid-template-columns: 1fr;
    gap: 5px;
  }

  .admin-ranking-rule-order {
    grid-column: 2;
  }

  .admin-ranking-rule-remove {
    grid-column: 1;
    grid-row: 2;
  }
}
</style>
