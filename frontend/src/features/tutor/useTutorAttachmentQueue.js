import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { deleteAttachment, getAttachment, uploadAttachments } from '../../services/tutorAttachmentsApi'

const MAX_FILES = 5
const MAX_PROCESSING_POLLS = 40
const PROCESSING_POLL_MS = 100

function wait(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

export function useTutorAttachmentQueue({ conversationId, onError } = {}) {
  const [attachments, setAttachments] = useState([])
  const pendingRef = useRef([])
  const runningRef = useRef(0)
  const mountedRef = useRef(true)
  const attachmentsRef = useRef([])
  const onErrorRef = useRef(onError)
  useEffect(() => {
    attachmentsRef.current = attachments
  }, [attachments])
  useEffect(() => {
    onErrorRef.current = onError
  }, [onError])

  const updateAttachment = useCallback((localId, patch) => {
    if (!mountedRef.current) return
    setAttachments((current) => current.map((item) => (
      item.localId === localId ? { ...item, ...patch } : item
    )))
  }, [])

  const pollUntilReady = useCallback(async (localId, id) => {
    for (let attempt = 0; attempt < MAX_PROCESSING_POLLS; attempt += 1) {
      const current = await getAttachment(id)
      if (current?.status === 'READY' || current?.status === 'IMAGE_READY') return current
      if (current?.status === 'FAILED' || current?.status === 'EXPIRED') {
        throw new Error(current.errorMessage || 'Tệp đính kèm không thể xử lý.')
      }
      updateAttachment(localId, { status: current?.status || 'PROCESSING' })
      await wait(PROCESSING_POLL_MS)
    }
    throw new Error('Tệp đính kèm xử lý quá lâu. Vui lòng thử lại.')
  }, [updateAttachment])

  const processEntry = useCallback(async (entry) => {
    try {
      const result = await uploadAttachments([entry.file], entry.conversationId || conversationId)
      const uploaded = result?.[0]
      if (!uploaded?.id) throw new Error('Tải tệp không trả về mã hợp lệ.')
      updateAttachment(entry.localId, { ...uploaded, id: uploaded.id, status: uploaded.status || 'READY' })
      const finalRecord = uploaded.status === 'PROCESSING'
        ? await pollUntilReady(entry.localId, uploaded.id)
        : uploaded
      updateAttachment(entry.localId, { ...finalRecord, id: uploaded.id, file: entry.file, previewUrl: entry.previewUrl })
    } catch (error) {
      updateAttachment(entry.localId, {
        status: 'FAILED',
        errorMessage: error?.message || 'Tải tệp đính kèm thất bại.',
      })
      onErrorRef.current?.(error)
    }
  }, [conversationId, pollUntilReady, updateAttachment])

  const drain = useCallback(() => {
    function runQueue() {
      while (runningRef.current < 2 && pendingRef.current.length > 0) {
        const entry = pendingRef.current.shift()
        runningRef.current += 1
        processEntry(entry).finally(() => {
          runningRef.current -= 1
          runQueue()
        })
      }
    }
    runQueue()
  }, [processEntry])

  const addFiles = useCallback((files, conversationIdOverride = null) => {
    const selected = Array.from(files || [])
    if (selected.length === 0) return
    if (attachments.length + selected.length > MAX_FILES) {
      onErrorRef.current?.({ code: 'ATTACHMENT_LIMIT_EXCEEDED', message: 'Chỉ được đính kèm tối đa 5 tệp.' })
      return
    }
    const entries = selected.map((file, index) => {
      let previewUrl = null
      if (file.type?.startsWith('image/') && typeof URL.createObjectURL === 'function') {
        try { previewUrl = URL.createObjectURL(file) } catch { previewUrl = null }
      }
      return {
        localId: `att-temp-${Date.now()}-${index}-${Math.random().toString(36).slice(2, 7)}`,
        id: null,
        filename: file.name,
        sizeBytes: file.size,
        status: 'UPLOADING',
        conversationId: conversationIdOverride || conversationId,
        file,
        previewUrl,
      }
    })
    setAttachments((current) => [...current, ...entries])
    pendingRef.current.push(...entries)
    drain()
  }, [attachments.length, conversationId, drain])

  const retry = useCallback((localId) => {
    const entry = attachments.find((item) => item.localId === localId)
    if (!entry || entry.status !== 'FAILED') return
    pendingRef.current.push({ ...entry, status: 'UPLOADING' })
    setAttachments((current) => current.map((item) => item.localId === localId
      ? { ...item, status: 'UPLOADING', errorMessage: null }
      : item))
    drain()
  }, [attachments, drain])

  const remove = useCallback((localId) => {
    const entry = attachments.find((item) => item.localId === localId)
    if (!entry) return
    pendingRef.current = pendingRef.current.filter((item) => item.localId !== localId)
    if (entry.previewUrl && typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(entry.previewUrl)
    setAttachments((current) => current.filter((item) => item.localId !== localId))
    if (entry.id && !String(entry.id).startsWith('att-temp-')) Promise.resolve(deleteAttachment(entry.id)).catch(() => {})
  }, [attachments])

  useEffect(() => () => {
    mountedRef.current = false
    pendingRef.current = []
    attachmentsRef.current.forEach((entry) => {
      if (entry.previewUrl && typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(entry.previewUrl)
    })
  }, [])

  const readyIds = useMemo(() => attachments
    .filter((item) => (item.status === 'READY' || item.status === 'IMAGE_READY') && item.id)
    .map((item) => item.id), [attachments])
  const isBusy = attachments.some((item) => ['UPLOADING', 'PROCESSING'].includes(item.status))
  const reportError = useCallback((error) => onErrorRef.current?.(error), [])

  return { attachments, addFiles, retry, remove, readyIds, isBusy, reportError, markChatRetry: () => {} }
}

export default useTutorAttachmentQueue
