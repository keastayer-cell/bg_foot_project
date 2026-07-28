import { describe, expect, it } from 'vitest'
import { useConfirmDialog } from './useConfirmDialog'

describe('useConfirmDialog', () => {
  it('resolves true after confirmation', async () => {
    const dialog = useConfirmDialog()
    const result = dialog.confirmAction({
      title: 'Удалить матч?',
      message: 'Действие нельзя отменить.',
      confirmLabel: 'Удалить',
    })

    expect(dialog.isOpen.value).toBe(true)
    expect(dialog.options.value.title).toBe('Удалить матч?')

    dialog.accept()

    await expect(result).resolves.toBe(true)
    expect(dialog.isOpen.value).toBe(false)
  })

  it('resolves false after cancellation', async () => {
    const dialog = useConfirmDialog()
    const result = dialog.confirmAction()

    dialog.cancel()

    await expect(result).resolves.toBe(false)
  })

  it('cancels an earlier request when a new one opens', async () => {
    const dialog = useConfirmDialog()
    const firstResult = dialog.confirmAction({ title: 'Первое действие' })
    const secondResult = dialog.confirmAction({ title: 'Второе действие' })

    await expect(firstResult).resolves.toBe(false)
    expect(dialog.options.value.title).toBe('Второе действие')

    dialog.cancel()
    await expect(secondResult).resolves.toBe(false)
  })
})
