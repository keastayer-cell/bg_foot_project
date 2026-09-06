<template>
  <nav class="entity-mode-switch" :aria-label="label">
    <button
      v-for="(option, index) in options"
      :key="option.value"
      class="entity-mode-button"
      :class="{ active: modelValue === option.value }"
      type="button"
      :aria-current="modelValue === option.value ? 'step' : undefined"
      @click="$emit('update:modelValue', option.value)"
    >
      <span>{{ String(index + 1).padStart(2, '0') }}</span>
      <strong>{{ option.label }}</strong>
      <small>{{ option.description }}</small>
    </button>
  </nav>
</template>

<script setup>
defineProps({
  label: { type: String, default: 'Режим работы' },
  modelValue: { type: String, required: true },
  options: { type: Array, required: true },
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.entity-mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 260px));
  gap: 8px;
  width: fit-content;
  padding: 5px;
  border: 1px solid rgba(124, 163, 255, 0.16);
  border-radius: 10px;
  background: rgba(8, 14, 32, 0.72);
}

.entity-mode-button {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 2px 9px;
  min-width: 220px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: var(--muted);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.entity-mode-button > span {
  grid-row: 1 / 3;
  color: rgba(151, 176, 255, 0.64);
  font-size: 0.7rem;
  font-weight: 800;
}

.entity-mode-button strong { color: #dfe7ff; font-size: 0.84rem; }
.entity-mode-button small { font-size: 0.7rem; }

.entity-mode-button:hover { background: rgba(124, 163, 255, 0.07); }
.entity-mode-button.active { border-color: rgba(97, 232, 162, 0.3); background: rgba(97, 232, 162, 0.09); }
.entity-mode-button.active > span, .entity-mode-button.active strong { color: var(--brand); }

@media (max-width: 600px) {
  .entity-mode-switch { grid-template-columns: 1fr; width: 100%; }
  .entity-mode-button { min-width: 0; width: 100%; }
}
</style>
