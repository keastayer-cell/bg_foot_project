import { reactive, ref } from 'vue'

export function useIncomingTransfers({ api, pageError, pageSuccess, onProcessed }) {
  const incomingTransfersLoading = ref(false)
  const incomingTransfersModalOpen = ref(false)
  const incomingDecisionLoadingId = ref(null)
  const incomingTransfersSummary = ref(emptySummary())
  const incomingDecisionComments = reactive({})

  async function loadIncomingTransfersNotifications(pageNum = 0) {
    incomingTransfersLoading.value = true
    try {
      const payload = await api.getIncomingTransfers(pageNum)
      incomingTransfersSummary.value = {
        totalPendingCount: Number(payload?.totalPendingCount || 0),
        requests: Array.isArray(payload?.requests) ? payload.requests : [],
        pageNumber: Number(payload?.pageNumber || 0),
        pageSize: Number(payload?.pageSize || 20),
        totalElements: Number(payload?.totalElements || 0),
        totalPages: Number(payload?.totalPages || 0),
      }
    } catch (error) {
      pageError.value = error.message || 'Не удалось загрузить входящие трансферные заявки.'
    } finally {
      incomingTransfersLoading.value = false
    }
  }

  async function openIncomingTransfersModal() {
    incomingTransfersModalOpen.value = true
    await loadIncomingTransfersNotifications(0)
  }

  function closeIncomingTransfersModal() {
    incomingTransfersModalOpen.value = false
  }

  async function changeIncomingTransfersPage(pageNum) {
    if (pageNum < 0) return
    if (
      incomingTransfersSummary.value.totalPages
      && pageNum >= incomingTransfersSummary.value.totalPages
    ) return
    await loadIncomingTransfersNotifications(pageNum)
  }

  async function processIncomingTransfer(requestId, action) {
    incomingDecisionLoadingId.value = requestId
    pageError.value = ''
    pageSuccess.value = ''

    try {
      await api.processIncomingTransfer(
        requestId,
        action,
        incomingDecisionComments[requestId],
      )
      incomingDecisionComments[requestId] = ''
      pageSuccess.value = action === 'approve' ? 'Трансфер подтвержден.' : 'Трансфер отклонен.'
      await loadIncomingTransfersNotifications(incomingTransfersSummary.value.pageNumber || 0)
      await onProcessed?.()
    } catch (error) {
      pageError.value = error.message || 'Не удалось обработать входящую заявку.'
    } finally {
      incomingDecisionLoadingId.value = null
    }
  }

  return {
    incomingTransfersLoading,
    incomingTransfersModalOpen,
    incomingDecisionLoadingId,
    incomingTransfersSummary,
    incomingDecisionComments,
    loadIncomingTransfersNotifications,
    openIncomingTransfersModal,
    closeIncomingTransfersModal,
    changeIncomingTransfersPage,
    processIncomingTransfer,
  }
}

function emptySummary() {
  return {
    totalPendingCount: 0,
    requests: [],
    pageNumber: 0,
    pageSize: 20,
    totalElements: 0,
    totalPages: 0,
  }
}
