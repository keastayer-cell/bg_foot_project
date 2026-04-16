<template>
  <div ref="rootRef" class="searchable-select" :class="{ 'is-open': isOpen, 'is-disabled': disabled }">
    <div class="searchable-select-control">
      <input
        class="searchable-select-input"
        :value="inputValue"
        :placeholder="selectedOption && !isOpen ? selectedOption.label : placeholder"
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
      <div v-if="filteredOptions.length" class="searchable-select-options">
        <button
          v-for="option in filteredOptions"
          :key="option.value"
          class="searchable-select-option"
          :class="{ 'is-selected': option.value === modelValue, 'is-disabled': option.disabled }"
          type="button"
          :disabled="option.disabled"
          @click="selectOption(option)"
        >
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
    type: String,
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

const selectedOption = computed(() => {
  return normalizedOptions.value.find((option) => option.value === String(props.modelValue || '')) || null
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
  return selectedOption.value?.label || ''
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
  emit('update:modelValue', option.value)
  closeDropdown()
}

function handleDocumentPointer(event) {
  if (!rootRef.value) {
    return
  }
  if (!rootRef.value.contains(event.target)) {
    closeDropdown()
  }
}

if (typeof document !== 'undefined') {
  document.addEventListener('pointerdown', handleDocumentPointer)
}

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') {
    document.removeEventListener('pointerdown', handleDocumentPointer)
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
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.12));
  border-radius: 12px 0 0 12px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--text);
}

.searchable-select-input:focus-visible {
  outline: none;
  border-color: rgba(97, 232, 162, 0.62);
}

.searchable-select-toggle {
  min-width: 44px;
  border: 1px solid var(--border, rgba(255, 255, 255, 0.12));
  border-left: none;
  border-radius: 0 12px 12px 0;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text);
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
  border: 1px solid rgba(124, 163, 255, 0.2);
  border-radius: 14px;
  background: rgba(11, 18, 39, 0.98);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.28);
}

.searchable-select-hint,
.searchable-select-empty {
  margin: 0;
  color: var(--muted);
  font-size: 0.82rem;
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
  gap: 2px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
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
  color: var(--muted);
  font-size: 0.78rem;
}

.searchable-select.is-disabled {
  opacity: 0.72;
}
</style>