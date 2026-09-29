import { useCallback, useEffect, useMemo, useReducer, useRef } from 'react'
import { deleteAttachment, getAttachment, uploadAttachments } from '../../services/tutorAttachmentsApi'
import { validateAttachmentFile } from './attachmentContract'

const MAX_FILES = 5
const MAX_CONCURRENT_UPLOADS = 2
const MAX_PROCESSING_POLLS = 40
const PROCESSING_POLL_MS = 100
const DEFAULT_VALIDATION_TIMEOUT_MS = 15000
const VALIDATION_TIMEOUT_ERROR = 'Không thể kiểm tra tệp. Thử lại.'
const CONVERSATION_REQUIRED_ERROR = 'Cần một cuộc hội thoại để tải tệp.'
const ACTIVE_STATUSES = new Set(['SELECTED', 'VALIDATING', 'UPLOADING', 'STORED', 'PROCESSING'])

const ALLOWED_TRANSITIONS = Object.freeze({
  SELECTED: new Set(['VALIDATING', 'FAILED', 'REMOVED']),
  VALIDATING: new Set(['UPLOADING', 'FAILED', 'REMOVED']),
  UPLOADING: new Set(['STORED', 'PROCESSING', 'READY', 'FAILED', 'REMOVED']),
  STORED: new Set(['PROCESSING', 'READY', 'FAILED', 'REMOVED']),
  PROCESSING: new Set(['READY', 'FAILED', 'REMOVED']),
  READY: new Set(['REMOVED']),
  FAILED: new Set(['SELECTED', 'REMOVED']),
  REMOVED: new Set(),
})

function canTransition(from, to) {
  return from === to || ALLOWED_TRANSITIONS[from]?.has(to)
}

export function attachmentReducer(state, action) {
  switch (action.type) {
    case 'ENQUEUE':
      return [...state, ...action.entries]
    case 'TRANSITION':
      return state.map((item) => {
        if (item.localId !== action.localId || !canTransition(item.status, action.status)) return item
        return { ...item, ...action.patch, status: action.status }
      })
    case 'REMOVE':
      return state.map((item) => {
        if (item.localId !== action.localId || !canTransition(item.status, 'REMOVED')) return item
        return { ...item, status: 'REMOVED' }
      })
    default:
      return state
  }
}

function wait(ms, signal) {
  return new Promise((resolve) => {
    let timer = null
    const cleanup = () => signal?.removeEventListener('abort', handleAbort)
    const handleAbort = () => {
      if (timer) window.clearTimeout(timer)
      cleanup()
      resolve(false)
    }
    timer = window.setTimeout(() => {
      cleanup()
      resolve(true)
    }, ms)
    if (signal?.aborted) handleAbort()
    else signal?.addEventListener('abort', handleAbort, { once: true })
  })
}

function validateWithTimeout(file, validateFile, timeoutMs) {
  if (timeoutMs <= 0) return Promise.resolve().then(() => validateFile(file))
  let timer = null
  return Promise.race([
    Promise.resolve().then(() => validateFile(file)),
    new Promise((_, reject) => {
      timer = window.setTimeout(() => reject(new Error(VALIDATION_TIMEOUT_ERROR)), timeoutMs)
    }),
  ]).finally(() => {
    if (timer) window.clearTimeout(timer)
  })
}

function normalizeServerStatus(status) {
  if (status === 'READY' || status === 'IMAGE_READY') return 'READY'
  if (status === 'STORED') return 'STORED'
  return 'PROCESSING'
}

export function useTutorAttachmentQueue({
  conversationId,
  ensureConversation,
  onError,
  validateFile = validateAttachmentFile,
  validationTimeoutMs = DEFAULT_VALIDATION_TIMEOUT_MS,
} = {}) {
  const [attachmentState, dispatch] = useReducer(attachmentReducer, [])
  const attachments = useMemo(
    () => attachmentState.filter((item) => item.status !== 'REMOVED'),
    [attachmentState],
  )
  const attachmentsRef = useRef([])
  const mountedRef = useRef(true)
  const conversationIdRef = useRef(conversationId || null)
  const ensureConversationRef = useRef(ensureConversation)
  const onErrorRef = useRef(onError)
  const conversationPromiseRef = useRef(null)
  const pendingTasksRef = useRef([])
  const runtimeRef = useRef(new Map())
  const runningRef = useRef(0)

  const dispatchState = useCallback((action) => {
    if (!mountedRef.current) return
    attachmentsRef.current = attachmentReducer(attachmentsRef.current, action)
    dispatch(action)
  }, [])

  useEffect(() => {
    conversationIdRef.current = conversationId || null
  }, [conversationId])

  useEffect(() => {
    ensureConversationRef.current = ensureConversation
  }, [ensureConversation])

  useEffect(() => {
    onErrorRef.current = onError
  }, [onError])

  const transitionAttachment = useCallback((localId, status, patch = {}) => {
    const current = attachmentsRef.current.find((item) => item.localId === localId)
    if (!current || !canTransition(current.status, status)) return false
    dispatchState({ type: 'TRANSITION', localId, status, patch })
    return true
  }, [dispatchState])

  const ensureQueueConversation = useCallback(() => {
    if (conversationIdRef.current) return Promise.resolve(conversationIdRef.current)
    if (conversationPromiseRef.current) return conversationPromiseRef.current

    const creator = ensureConversationRef.current
    if (typeof creator !== 'function') return Promise.resolve(null)

    conversationPromiseRef.current = Promise.resolve()
      .then(() => creator())
      .then((id) => {
        if (id) conversationIdRef.current = id
        return id || null
      })
      .finally(() => {
        conversationPromiseRef.current = null
      })

    return conversationPromiseRef.current
  }, [])

  const pollUntilReady = useCallback(async (localId, id, signal) => {
    for (let attempt = 0; attempt < MAX_PROCESSING_POLLS; attempt += 1) {
      if (signal.aborted) return null
      const current = await getAttachment(id, { signal })
      if (signal.aborted) return null
      if (current?.status === 'READY' || current?.status === 'IMAGE_READY') return current
      if (current?.status === 'FAILED' || current?.status === 'EXPIRED') {
        throw new Error(current.errorMessage || 'Tệp đính kèm không thể xử lý.')
      }
      const nextStatus = current?.status === 'STORED' ? 'STORED' : 'PROCESSING'
      transitionAttachment(localId, nextStatus, { ...current, id })
      if (!(await wait(PROCESSING_POLL_MS, signal)) || signal.aborted) return null
    }
    throw new Error('Tệp đính kèm xử lý quá lâu. Vui lòng thử lại.')
  }, [transitionAttachment])

  async function processEntry(localId) {
    const runtime = runtimeRef.current.get(localId)
    const entry = attachmentsRef.current.find((item) => item.localId === localId)
    if (!runtime || !entry || runtime.removed || !mountedRef.current) return

    transitionAttachment(localId, 'VALIDATING')
    let validation
    try {
      validation = await validateWithTimeout(entry.file, validateFile, validationTimeoutMs)
    } catch (error) {
      if (!runtime.removed && mountedRef.current) {
        transitionAttachment(localId, 'FAILED', { errorMessage: error?.message || VALIDATION_TIMEOUT_ERROR })
        onErrorRef.current?.({ code: 'ATTACHMENT_VALIDATION_TIMEOUT', message: error?.message || VALIDATION_TIMEOUT_ERROR })
      }
      return
    }

    if (runtime.removed || !mountedRef.current) return
    if (!validation?.valid) {
      transitionAttachment(localId, 'FAILED', { errorMessage: validation?.error?.message || 'Định dạng tệp không được hỗ trợ.' })
      onErrorRef.current?.(validation?.error)
      return
    }

    let targetConversationId = entry.conversationId || conversationIdRef.current
    if (!targetConversationId) {
      try {
        targetConversationId = await ensureQueueConversation()
      } catch (error) {
        if (!runtime.removed && mountedRef.current) {
          transitionAttachment(localId, 'FAILED', { errorMessage: error?.message || 'Không thể tạo cuộc hội thoại.' })
          onErrorRef.current?.(error)
        }
        return
      }
    }

    if (runtime.removed || !mountedRef.current) return
    if (!targetConversationId) {
      transitionAttachment(localId, 'FAILED', { errorMessage: CONVERSATION_REQUIRED_ERROR })
      onErrorRef.current?.({ code: 'ATTACHMENT_CONVERSATION_REQUIRED', message: CONVERSATION_REQUIRED_ERROR })
      return
    }

    transitionAttachment(localId, 'UPLOADING', { conversationId: targetConversationId })
    try {
      const result = await uploadAttachments([entry.file], targetConversationId, { signal: runtime.controller.signal })
      const uploaded = result?.[0]
      if (!uploaded?.id) throw new Error('Tải tệp không trả về mã hợp lệ.')
      const uploadedStatus = normalizeServerStatus(uploaded.status)
      transitionAttachment(localId, uploadedStatus, {
        ...uploaded,
        id: uploaded.id,
        conversationId: targetConversationId,
        serverStatus: uploaded.status,
      })
      const finalRecord = uploadedStatus === 'STORED' || uploadedStatus === 'PROCESSING'
        ? await pollUntilReady(localId, uploaded.id, runtime.controller.signal)
        : uploaded
      if (!finalRecord || runtime.controller.signal.aborted || runtime.removed) return
      transitionAttachment(localId, 'READY', {
        ...finalRecord,
        id: uploaded.id,
        file: entry.file,
        previewUrl: entry.previewUrl,
        serverStatus: finalRecord.status,
      })
    } catch (error) {
      if (runtime.controller.signal.aborted || runtime.removed || !mountedRef.current) return
      transitionAttachment(localId, 'FAILED', { errorMessage: error?.message || 'Tải tệp đính kèm thất bại.' })
      onErrorRef.current?.(error)
    }
  }

  function drainQueue() {
    while (runningRef.current < MAX_CONCURRENT_UPLOADS && pendingTasksRef.current.length > 0) {
      const localId = pendingTasksRef.current.shift()
      const runtime = runtimeRef.current.get(localId)
      if (!runtime || runtime.removed) continue
      runningRef.current += 1
      runtime.task = processEntry(localId).finally(() => {
        runningRef.current -= 1
        runtimeRef.current.delete(localId)
        drainQueue()
      })
    }
  }

  function addFiles(files, conversationIdOverride = null) {
    const selected = Array.from(files || [])
    if (selected.length === 0) return
    const activeCount = attachmentsRef.current.filter((item) => ACTIVE_STATUSES.has(item.status)).length
    if (activeCount + selected.length > MAX_FILES) {
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
        status: 'SELECTED',
        conversationId: conversationIdOverride || conversationIdRef.current || null,
        file,
        previewUrl,
      }
    })

    dispatchState({ type: 'ENQUEUE', entries })
    entries.forEach((entry) => {
      runtimeRef.current.set(entry.localId, {
        controller: new AbortController(),
        removed: false,
        task: null,
      })
      pendingTasksRef.current.push(entry.localId)
    })
    drainQueue()
  }

  function retry(localId) {
    const entry = attachmentsRef.current.find((item) => item.localId === localId)
    if (!entry || entry.status !== 'FAILED') return
    dispatchState({ type: 'TRANSITION', localId, status: 'SELECTED', patch: { errorMessage: null } })
    runtimeRef.current.set(localId, { controller: new AbortController(), removed: false, task: null })
    pendingTasksRef.current.push(localId)
    drainQueue()
  }

  function failPending(error) {
    const message = error?.message || 'Không thể tạo cuộc hội thoại.'
    runtimeRef.current.forEach((runtime, localId) => {
      const current = attachmentsRef.current.find((item) => item.localId === localId)
      if (current && ACTIVE_STATUSES.has(current.status)) {
        runtime.controller.abort()
        runtime.removed = true
        transitionAttachment(localId, 'FAILED', { errorMessage: message })
      }
    })
    pendingTasksRef.current = []
    onErrorRef.current?.(error)
  }

  const remove = useCallback((localId) => {
    const entry = attachmentsRef.current.find((item) => item.localId === localId)
    if (!entry) return
    const runtime = runtimeRef.current.get(localId)
    if (runtime) {
      runtime.removed = true
      runtime.controller.abort()
      runtimeRef.current.delete(localId)
    }
    pendingTasksRef.current = pendingTasksRef.current.filter((id) => id !== localId)
    if (entry.previewUrl && typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(entry.previewUrl)
    dispatchState({ type: 'REMOVE', localId })
    if (entry.id && !String(entry.id).startsWith('att-temp-')) Promise.resolve(deleteAttachment(entry.id)).catch(() => {})
  }, [dispatchState])

  useEffect(() => () => {
    mountedRef.current = false
    pendingTasksRef.current = []
    runtimeRef.current.forEach((runtime) => runtime.controller.abort())
    runtimeRef.current.clear()
    attachmentsRef.current.forEach((entry) => {
      if (entry.previewUrl && typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(entry.previewUrl)
    })
  }, [])

  const readyIds = useMemo(() => attachments
    .filter((item) => item.status === 'READY' && item.id)
    .map((item) => item.id), [attachments])
  const isBusy = attachments.some((item) => ['VALIDATING', 'UPLOADING', 'STORED', 'PROCESSING'].includes(item.status))
  const reportError = useCallback((error) => onErrorRef.current?.(error), [])

  return {
    attachments,
    addFiles,
    retry,
    remove,
    readyIds,
    isBusy,
    reportError,
    failPending,
    markChatRetry: () => {},
  }
}

export default useTutorAttachmentQueue
