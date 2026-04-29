<template>
  <div ref="rootRef" class="searchable-select" :class="{ 'is-open': isOpen, 'is-disabled': disabled, 'is-multiple': multiple }">
    <div class="searchable-select-control">
      <input
        class="searchable-select-input"
        :value="inputValue"
        :placeholder="resolvedPlaceholder"
        :disabled="disabled"
        autocomplete="off"
        @focus="openDropdown"
        @input="handleInput"
        @keydown.down.prevent="openDropdown"
        @keydown.esc.prevent="closeDropdown"
      />
      <button
        class="searchable-select-toggle"
        type="button"
        :disabled="disabled"
        @click="toggleDropdown"
      >
        <span class="searchable-select-chevron">▾</span>
      </button>
    </div>

    <div v-if="isOpen" class="searchable-select-dropdown">
      <p class="searchable-select-hint">{{ searchPlaceholder }}</p>
      <p v-if="multiple && multipleActionHint" class="searchable-select-action-hint">{{ multipleActionHint }}</p>
      <div v-if="filteredOptions.length" class="searchable-select-options">
        <button
          v-for="option in filteredOptions"
          :key="option.value"
          class="searchable-select-option"
          :class="{ 'is-selected': isOptionSelected(option.value), 'is-disabled': option.disabled }"
          type="button"
          :disabled="option.disabled"
          @click="selectOption(option)"
        >
          <span v-if="multiple" class="searchable-select-option-check">{{ isOptionSelected(option.value) ? '✓' : '' }}</span>
          <span class="searchable-select-option-label">{{ option.label }}</span>
          <span v-if="option.caption" class="searchable-select-option-caption">{{ option.caption }}</span>
        </button>
      </div>
      <p v-else class="searchable-select-empty">{{ emptyText }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Array],
    default: '',
  },
  options: {
    type: Array,
    default: () => [],
  },
  placeholder: {
    type: String,
    default: 'Выберите значение',
  },
  searchPlaceholder: {
    type: String,
    default: 'Начните вводить для поиска',
  },
  emptyText: {
    type: String,
    default: 'Ничего не найдено',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  multiple: {
    type: Boolean,
    default: false,
  },
  multipleSummaryText: {
    type: String,
    default: 'Выбрано',
  },
  multipleActionHint: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const rootRef = ref(null)
const isOpen = ref(false)
const query = ref('')

const normalizedOptions = computed(() => {
  return (props.options || []).map((option) => ({
    value: String(option?.value ?? ''),
    label: String(option?.label ?? ''),
    caption: option?.caption ? String(option.caption) : '',
    disabled: Boolean(option?.disabled),
    keywords: String(option?.keywords ?? ''),
  }))
})

const selectedValues = computed(() => {
  if (!props.multiple) {
    return [String(props.modelValue || '')].filter(Boolean)
  }
  return Array.isArray(props.modelValue)
    ? props.modelValue.map((value) => String(value || '')).filter(Boolean)
    : []
})

const selectedOption = computed(() => {
  return normalizedOptions.value.find((option) => option.value === String(props.modelValue || '')) || null
})

const selectedOptions = computed(() => {
  const values = new Set(selectedValues.value)
  return normalizedOptions.value.filter((option) => values.has(option.value))
})

const filteredOptions = computed(() => {
  const normalizedQuery = normalize(query.value)
  if (!normalizedQuery) {
    return normalizedOptions.value
  }

  return normalizedOptions.value.filter((option) => {
    return [option.label, option.caption, option.keywords]
      .map(normalize)
      .some((value) => value.includes(normalizedQuery))
  })
})

const inputValue = computed(() => {
  if (isOpen.value) {
    return query.value
  }
  if (props.multiple) {
    if (!selectedOptions.value.length) {
      return ''
    }
    if (selectedOptions.value.length === 1) {
      return selectedOptions.value[0].label
    }
    return `${props.multipleSummaryText}: ${selectedOptions.value.length}`
  }
  return selectedOption.value?.label || ''
})

const resolvedPlaceholder = computed(() => {
  if (props.multiple) {
    return props.placeholder
  }
  return selectedOption.value && !isOpen.value ? selectedOption.value.label : props.placeholder
})

watch(
  () => props.modelValue,
  () => {
    if (!isOpen.value) {
      query.value = ''
    }
  }
)

function normalize(value) {
  return String(value || '').trim().toLowerCase()
}

function openDropdown() {
  if (props.disabled) {
    return
  }
  isOpen.value = true
  query.value = ''
}

function closeDropdown() {
  isOpen.value = false
  query.value = ''
}

function toggleDropdown() {
  if (isOpen.value) {
    closeDropdown()
    return
  }
  openDropdown()
}

function handleInput(event) {
  if (!isOpen.value) {
    isOpen.value = true
  }
  query.value = String(event?.target?.value || '')
}

function selectOption(option) {
  if (option.disabled) {
    return
  }
  if (props.multiple) {
    const nextValues = new Set(selectedValues.value)
    if (nextValues.has(option.value)) {
      nextValues.delete(option.value)
    } else {
      nextValues.add(option.value)
    }
    emit('update:modelValue', Array.from(nextValues))
    query.value = ''
    return
  }
  emit('update:modelValue', option.value)
  closeDropdown()
}

function isOptionSelected(value) {
  if (props.multiple) {
    return selectedValues.value.includes(String(value || ''))
  }
  return String(props.modelValue || '') === String(value || '')
}

function handleDocumentClick(event) {
  if (!rootRef.value) {
    return
  }
  if (!rootRef.value.contains(event.target)) {
    closeDropdown()
  }
}

if (typeof document !== 'undefined') {
  document.addEventListener('click', handleDocumentClick)
}

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') {
    document.removeEventListener('click', handleDocumentClick)
  }
})
</script>

<style scoped>
.searchable-select {
  position: relative;
  width: 100%;
}

.searchable-select-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: stretch;
}

.searchable-select-input {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid var(--field-border, rgba(124, 163, 255, 0.28));
  border-radius: 12px 0 0 12px;
  background: var(--field-bg, linear-gradient(180deg, rgba(22, 31, 67, 0.96), rgba(14, 21, 47, 0.98)));
  color: var(--text);
  box-shadow: var(--field-shadow, inset 0 1px 0 rgba(255, 255, 255, 0.05), 0 8px 18px rgba(5, 10, 28, 0.16));
}

.searchable-select-input:focus-visible {
  outline: none;
  border-color: rgba(97, 232, 162, 0.62);
}

.searchable-select-toggle {
  min-width: 48px;
  border: 1px solid var(--field-border, rgba(124, 163, 255, 0.28));
  border-left: none;
  border-radius: 0 12px 12px 0;
  background: var(--field-bg, linear-gradient(180deg, rgba(22, 31, 67, 0.96), rgba(14, 21, 47, 0.98)));
  color: var(--text);
  box-shadow: var(--field-shadow, inset 0 1px 0 rgba(255, 255, 255, 0.05), 0 8px 18px rgba(5, 10, 28, 0.16));
}

.searchable-select-chevron {
  font-size: 0.92rem;
}

.searchable-select-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 30;
  padding: 10px;
  border: 1px solid rgba(124, 163, 255, 0.24);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(16, 24, 53, 0.98), rgba(10, 16, 37, 1));
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.34);
  backdrop-filter: blur(12px);
}

.searchable-select.is-multiple.is-open .searchable-select-dropdown {
  position: static;
  top: auto;
  left: auto;
  right: auto;
  margin-top: 8px;
}

.searchable-select-hint,
.searchable-select-empty {
  margin: 0;
  color: var(--muted);
  font-size: 0.82rem;
}

.searchable-select-action-hint {
  margin: 6px 0 0;
  color: rgba(97, 232, 162, 0.92);
  font-size: 0.8rem;
  font-weight: 600;
}

.searchable-select-options {
  display: grid;
  gap: 6px;
  max-height: 260px;
  margin-top: 10px;
  overflow-y: auto;
}

.searchable-select-option {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 2px 10px;
  width: 100%;
  padding: 11px 12px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.035);
  color: var(--text);
  text-align: left;
}

.searchable-select-option:hover:not(.is-disabled),
.searchable-select-option.is-selected {
  border-color: rgba(97, 232, 162, 0.34);
  background: rgba(97, 232, 162, 0.08);
}

.searchable-select-option.is-disabled {
  opacity: 0.55;
}

.searchable-select-option-caption {
  grid-column: 2;
  color: var(--muted);
  font-size: 0.78rem;
}

.searchable-select-option-check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  min-width: 18px;
  color: rgba(97, 232, 162, 0.92);
  font-weight: 700;
}

.searchable-select.is-disabled {
  opacity: 0.72;
}
</style>