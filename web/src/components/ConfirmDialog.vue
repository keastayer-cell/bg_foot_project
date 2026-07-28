<template>
  <div
    v-if="isOpen"
    class="modal-backdrop confirm-backdrop"
    role="presentation"
    @click.self="cancel"
  >
    <section
      ref="dialogElement"
      class="confirm-dialog"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
      aria-describedby="confirm-dialog-message"
      tabindex="-1"
    >
      <div class="confirm-dialog-mark" :class="`is-${options.tone}`" aria-hidden="true">!</div>
      <div class="confirm-dialog-copy">
        <h2 id="confirm-dialog-title">{{ options.title }}</h2>
        <p id="confirm-dialog-message">{{ options.message }}</p>
      </div>
      <div class="confirm-dialog-actions">
        <button class="btn-ghost" type="button" @click="cancel">{{ options.cancelLabel }}</button>
        <button
          :class="options.tone === 'danger' ? 'btn-danger' : 'btn-primary'"
          type="button"
          @click="accept"
        >
          {{ options.confirmLabel }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useConfirmDialog } from '../composables/useConfirmDialog'

const { isOpen, options, accept, cancel } = useConfirmDialog()
const dialogElement = ref(null)

function handleKeydown(event) {
  if (event.key === 'Escape') {
    cancel()
  }
}

watch(isOpen, async (open) => {
  if (open) {
    document.addEventListener('keydown', handleKeydown)
    await nextTick()
    dialogElement.value?.focus()
  } else {
    document.removeEventListener('keydown', handleKeydown)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  cancel()
})
</script>
