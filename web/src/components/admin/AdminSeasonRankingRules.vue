<template>
  <section class="admin-season-section admin-season-section-compact admin-season-rules-panel">
    <div class="admin-season-section-head admin-season-section-head-compact">
      <div>
        <h4 class="admin-list-title">Приоритет критериев</h4>
        <p class="muted-text">Если команды равны, система последовательно переходит к следующему критерию.</p>
      </div>
      <button
        class="btn-ghost btn-sm"
        type="button"
        :disabled="form.rankingRules.length >= ruleOptions.length"
        @click="$emit('add')"
      >Добавить критерий</button>
    </div>

    <div class="admin-ranking-presets" aria-label="Готовые наборы критериев">
      <button
        v-for="preset in presets"
        :key="preset.value"
        class="admin-ranking-preset"
        :class="{ active: isPresetActive(preset) }"
        type="button"
        @click="$emit('apply-preset', preset.rules)"
      >
        <strong>{{ preset.label }}</strong>
        <small>{{ preset.description }}</small>
      </button>
    </div>

    <div class="admin-ranking-flow">
      <div class="admin-ranking-fixed-card">
        <span class="admin-ranking-rule-badge">1</span>
        <span><strong>Очки</strong><small>Основной показатель таблицы</small></span>
        <em>больше — выше</em>
      </div>

      <p v-if="!form.rankingRules.length" class="admin-ranking-empty">
        Добавьте хотя бы один критерий или выберите готовый набор.
      </p>
    </div>

    <div v-if="form.rankingRules.length" class="admin-ranking-rule-list">
      <div v-for="(rule, index) in form.rankingRules" :key="index" class="admin-ranking-rule-card">
        <span class="admin-ranking-rule-badge">{{ index + 2 }}</span>
        <label class="admin-ranking-rule-field">
          <select v-model="form.rankingRules[index]">
            <option value="">— выберите критерий —</option>
            <option
              v-for="option in availableOptions(index)"
              :key="`${index}-${option.value}`"
              :value="option.value"
            >{{ option.label }}</option>
          </select>
          <span v-if="selectedOption(rule)" class="admin-ranking-rule-description">
            {{ selectedOption(rule).description }}
          </span>
        </label>
        <span v-if="selectedOption(rule)" class="admin-ranking-direction">{{ selectedOption(rule).direction }}</span>
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

    <div class="admin-ranking-fixed-card admin-ranking-fixed-card-last">
      <span class="admin-ranking-rule-badge">{{ form.rankingRules.length + 2 }}</span>
      <span><strong>Название команды</strong><small>Технический порядок при полном равенстве</small></span>
      <em>А → Я</em>
    </div>

    <p class="admin-season-rules-summary"><span>Итоговый порядок</span><strong>{{ summary() }}</strong></p>
  </section>
</template>

<script setup>
const props = defineProps({
  availableOptions: { type: Function, required: true },
  form: { type: Object, required: true },
  presets: { type: Array, default: () => [] },
  ruleOptions: { type: Array, required: true },
  summary: { type: Function, required: true },
})

defineEmits(['add', 'apply-preset', 'move', 'remove'])

function selectedOption(rule) {
  return props.ruleOptions.find((option) => option.value === rule) || null
}

function isPresetActive(preset) {
  return preset.rules.length === props.form.rankingRules.length
    && preset.rules.every((rule, index) => rule === props.form.rankingRules[index])
}
</script>

<style scoped>
.admin-ranking-rule-card {
  grid-template-columns: auto minmax(0, 1fr) auto auto auto;
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
  display: grid;
  gap: 8px;
}

.admin-ranking-rule-card {
  min-height: 76px;
  padding: 10px 12px;
  border: 1px solid rgba(112, 145, 210, 0.18);
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(25, 39, 76, 0.68), rgba(15, 24, 51, 0.8));
}

.admin-ranking-rule-badge {
  width: 30px;
  height: 30px;
  border-radius: 50%;
}

.admin-ranking-rule-field {
  display: grid;
  gap: 5px;
}

.admin-ranking-rule-field select {
  max-width: 440px;
  min-height: 40px;
}

.admin-ranking-rule-description {
  color: var(--muted);
  font-size: 0.78rem;
  line-height: 1.35;
}

.admin-ranking-direction,
.admin-ranking-fixed-card em {
  padding: 5px 8px;
  border: 1px solid rgba(97, 232, 162, 0.2);
  border-radius: 999px;
  background: rgba(97, 232, 162, 0.08);
  color: #a8f2ca;
  font-size: 0.72rem;
  font-style: normal;
  white-space: nowrap;
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

.admin-ranking-presets {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  max-width: 900px;
}

.admin-ranking-preset {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid rgba(112, 145, 210, 0.2);
  border-radius: 12px;
  background: rgba(12, 20, 44, 0.62);
  color: var(--text);
  text-align: left;
  cursor: pointer;
}

.admin-ranking-preset small,
.admin-ranking-fixed-card small {
  display: block;
  margin-top: 3px;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 400;
}

.admin-ranking-preset:hover,
.admin-ranking-preset.active {
  border-color: rgba(97, 232, 162, 0.48);
  background: rgba(32, 75, 66, 0.34);
}

.admin-ranking-preset.active::after {
  content: 'Выбран';
  color: #9eebc2;
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-ranking-flow {
  max-width: 900px;
}

.admin-ranking-fixed-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  max-width: 876px;
  padding: 10px 12px;
  border: 1px dashed rgba(151, 176, 255, 0.25);
  border-radius: 12px;
  background: rgba(11, 18, 40, 0.46);
}

.admin-ranking-fixed-card-last {
  max-width: 876px;
}

.admin-ranking-empty {
  margin: 8px 0 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(255, 190, 92, 0.08);
  color: #e8d1a3;
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

.admin-season-rules-summary strong {
  font-weight: 700;
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

  .admin-ranking-direction {
    grid-column: 2;
    justify-self: start;
  }

  .admin-ranking-rule-remove {
    grid-column: 1;
    grid-row: 2;
  }

  .admin-ranking-presets {
    grid-template-columns: 1fr;
  }

  .admin-ranking-fixed-card {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .admin-ranking-fixed-card em {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
