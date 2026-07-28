<template>
  <section class="ui-state" :class="`ui-state--${tone}`" :role="tone === 'error' ? 'alert' : 'status'">
    <div class="ui-state-mark" aria-hidden="true">{{ mark }}</div>
    <div class="ui-state-copy">
      <h3>{{ title }}</h3>
      <p v-if="message">{{ message }}</p>
    </div>
    <button v-if="actionLabel" class="btn-ghost ui-state-action" type="button" @click="$emit('action')">
      {{ actionLabel }}
    </button>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tone: {
    type: String,
    default: 'empty',
    validator: (value) => ['empty', 'error', 'success', 'loading'].includes(value),
  },
  title: {
    type: String,
    required: true,
  },
  message: {
    type: String,
    default: '',
  },
  actionLabel: {
    type: String,
    default: '',
  },
})

defineEmits(['action'])

const mark = computed(() => ({
  empty: '—',
  error: '!',
  success: '✓',
  loading: '…',
})[props.tone])
</script>
