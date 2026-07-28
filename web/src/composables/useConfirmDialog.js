import { readonly, ref } from 'vue'

const isOpen = ref(false)
const options = ref({
  title: '',
  message: '',
  confirmLabel: 'Подтвердить',
  cancelLabel: 'Отмена',
  tone: 'danger',
})

let pendingResolve = null

function settle(result) {
  if (!isOpen.value) return

  isOpen.value = false
  const resolve = pendingResolve
  pendingResolve = null
  resolve?.(result)
}

export function useConfirmDialog() {
  function confirmAction(nextOptions = {}) {
    if (pendingResolve) {
      pendingResolve(false)
    }

    options.value = {
      title: nextOptions.title || 'Подтвердите действие',
      message: nextOptions.message || 'Это действие изменит данные.',
      confirmLabel: nextOptions.confirmLabel || 'Подтвердить',
      cancelLabel: nextOptions.cancelLabel || 'Отмена',
      tone: nextOptions.tone || 'danger',
    }
    isOpen.value = true

    return new Promise((resolve) => {
      pendingResolve = resolve
    })
  }

  return {
    isOpen: readonly(isOpen),
    options: readonly(options),
    confirmAction,
    accept: () => settle(true),
    cancel: () => settle(false),
  }
}
