<template>
  <section class="admin-season-section admin-season-section-compact admin-season-rules-panel">
    <div class="admin-season-section-head admin-season-section-head-compact">
      <div>
        <h4 class="admin-list-title">Тай-брейки таблицы</h4>
        <p class="muted-text">Критерии применяются последовательно после очков.</p>
      </div>
      <button
        class="btn-ghost btn-sm"
        type="button"
        :disabled="form.rankingRules.length >= ruleOptions.length"
        @click="$emit('add')"
      >Добавить критерий</button>
    </div>
    <p v-if="!form.rankingRules.length" class="muted-text">Дополнительные тай-брейки не выбраны.</p>
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
        <button class="btn-danger btn-sm" type="button" @click="$emit('remove', index)">Убрать</button>
      </div>
    </div>
    <div class="admin-season-rules-footer">
      <p class="muted-text">Сначала всегда считаются очки. Последним fallback используется алфавит.</p>
      <p class="muted-text admin-season-rules-summary">Текущий порядок: {{ summary() }}</p>
    </div>
  </section>
</template>

<script setup>
defineProps({
  availableOptions: { type: Function, required: true },
  form: { type: Object, required: true },
  ruleOptions: { type: Array, required: true },
  summary: { type: Function, required: true },
})

defineEmits(['add', 'remove'])
</script>
