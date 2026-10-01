import { useContext, useEffect, useRef, useState } from 'react'
import { UNSAFE_LocationContext } from 'react-router-dom'
import { X } from 'lucide-react'
import AiTutorMascotLauncher from './AiTutorMascotLauncher'
import TutorPanel from './TutorPanel'
import { SHELL_STATES } from './TutorShell'
import AuthGate from '../auth/AuthGate'
import { useOptionalAuth } from '../../features/auth/AuthProvider'
import { useOptionalPreferences } from '../../features/preferences/PreferenceProvider'
import { createTutorConversation, loadLatestTutorConversation, sendTutorMessage } from '../../services/aiTutorApi'
import { useTutorAttachmentQueue } from '../../features/tutor/useTutorAttachmentQueue'
import { ASSISTANT_NAME } from '../../features/tutor/assistantIdentity'

const welcomeMessage = {
  id: 'welcome',
  role: 'assistant',
  content: 'Mình có thể giúp bạn hiểu bài, xem lại lỗi và chọn bước luyện tập tiếp theo.',
}

function useSafeLocation() {
  const context = useContext(UNSAFE_LocationContext)
  return context?.location ?? null
}

const DEFAULT_CONTEXT = Object.freeze({ skill: 'GENERAL' })

function FloatingTutor({ context = DEFAULT_CONTEXT }) {
  const auth = useOptionalAuth()
  const preferenceContext = useOptionalPreferences()
  const location = useSafeLocation()
  const [open, setOpen] = useState(false)
  const [isLaunching, setIsLaunching] = useState(false)
  const [shellState, setShellState] = useState(SHELL_STATES.STANDARD)
  const [messages, setMessages] = useState([welcomeMessage])
  const [loading, setLoading] = useState(false)
  const [sessionExpired, setSessionExpired] = useState(false)
  const [activeContext, setActiveContext] = useState(context)
  const [conversationId, setConversationId] = useState(null)
  const buttonRef = useRef(null)
  const inputRef = useRef(null)
  const openedRef = useRef(false)
  const mountedRef = useRef(true)
  const abortControllerRef = useRef(null)
  const launchTimerRef = useRef(null)
  const conversationCreationRef = useRef(null)
  const initialPathRef = useRef(location?.pathname ?? '/')
  const accountKey = auth?.isAuthenticated ? auth.session?.user?.id ?? 'member' : 'guest'
  const proactiveSuggestionsEnabled = preferenceContext?.preferences.proactiveAiEnabled ?? true
  const attachmentQueue = useTutorAttachmentQueue({
    conversationId,
    ensureConversation: ensureTutorConversation,
  })
  const attachment = attachmentQueue.attachments[0] ?? null

  useEffect(() => {
    setMessages([welcomeMessage])
    setConversationId(null)
    setSessionExpired(false)
    if (!auth?.isAuthenticated) return undefined
    const controller = new AbortController()
    loadLatestTutorConversation({ signal: controller.signal }).then((conversation) => {
      if (!conversation || controller.signal.aborted || !mountedRef.current) return
      setConversationId(conversation.conversationId)
      setMessages(conversation.messages.length > 0 ? conversation.messages : [welcomeMessage])
    }).catch(() => {})
    return () => controller.abort()
  }, [accountKey, auth?.isAuthenticated])

  const isGuest = Boolean(auth && !auth.isAuthenticated) || sessionExpired

  useEffect(() => {
    setActiveContext(context)
    if (location?.pathname) {
      initialPathRef.current = location.pathname
    }
  }, [context, location?.pathname])

  useEffect(() => {
    if (
      context?.skill &&
      context.skill !== 'GENERAL' &&
      location?.pathname &&
      location.pathname !== initialPathRef.current
    ) {
      setActiveContext((prev) => (prev ? { ...prev, isStale: true } : prev))
    }
  }, [location?.pathname, context?.skill])

  useEffect(() => {
    if (!open) {
      if (openedRef.current) buttonRef.current?.focus()
      return undefined
    }

    openedRef.current = true
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const focusTimer = window.setTimeout(() => inputRef.current?.focus(), 0)

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        event.preventDefault()
        setOpen(false)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      window.clearTimeout(focusTimer)
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [open])

  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
      if (launchTimerRef.current) window.clearTimeout(launchTimerRef.current)
      if (abortControllerRef.current) {
        abortControllerRef.current.abort()
      }
    }
  }, [])

  function handleCancel() {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
      abortControllerRef.current = null
    }
    setLoading(false)
  }

  function handleClearContext() {
    setActiveContext({ skill: 'GENERAL' })
  }

  async function ensureTutorConversation() {
    let selectedConversationId = conversationId
    if (!selectedConversationId && auth?.isAuthenticated) {
      if (!conversationCreationRef.current) {
        conversationCreationRef.current = createTutorConversation(activeContext?.skill || 'general')
          .finally(() => { conversationCreationRef.current = null })
      }
      const conversation = await conversationCreationRef.current
      selectedConversationId = conversation.id
      setConversationId(selectedConversationId)
    }
    return selectedConversationId
  }

  async function handleAttachmentsSelected(files) {
    const selectedFiles = Array.from(files || [])
    if (selectedFiles.length === 0) return
    attachmentQueue.addFiles(selectedFiles, conversationId)
  }

  async function handleAttachmentSelected(file) {
    await handleAttachmentsSelected([file])
  }

  function handleAttachmentError(error) {
    attachmentQueue.reportError(error)
  }

  function handleRemoveAttachment() {
    if (attachment?.localId) attachmentQueue.remove(attachment.localId)
  }

  function handleRetryAttachment() {
    if (!attachment?.localId) return
    attachmentQueue.retry(attachment.localId)
  }

  function handleOpenTutor() {
    if (launchTimerRef.current) window.clearTimeout(launchTimerRef.current)
    setIsLaunching(true)
    setOpen(true)
    launchTimerRef.current = window.setTimeout(() => {
      launchTimerRef.current = null
      setIsLaunching(false)
    }, 180)
  }

  async function sendMessage(content, options = {}, retryState = null) {
    if (loading) return
    const attachmentIds = Array.from(options?.attachmentIds
      ?? (options?.attachmentId ? [options.attachmentId] : attachmentQueue.readyIds))
    const userMessageId = retryState?.userMessageId ?? `user-${Date.now()}`
    const errorMessageId = retryState?.errorMessageId ?? `assistant-error-${Date.now()}`
    const history = (!auth?.isAuthenticated || isGuest)
      ? messages.slice(-8).map((message) => ({
        role: message.role === 'user' ? 'USER' : 'ASSISTANT',
        content: message.content,
      }))
      : []
    if (!retryState) {
      setMessages((current) => [...current, { id: userMessageId, role: 'user', content }])
    } else {
      setMessages((current) => current.filter((message) => message.id !== errorMessageId))
    }
    setLoading(true)

    const controller = new AbortController()
    abortControllerRef.current = controller

    try {
      const selectedConversationId = await ensureTutorConversation()
      const response = await sendTutorMessage(
        {
          message: content,
          context: activeContext,
          history,
          conversationId: selectedConversationId,
          attachmentIds,
        },
        { signal: controller.signal },
      )
      if (!mountedRef.current) return
      if (response.conversationId) setConversationId(response.conversationId)
      setMessages((current) => [
        ...current,
        {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          status: response.status,
          content: response.answer,
          attachmentSources: response.attachmentSources ?? [],
          references: response.references ?? [],
          grounding:
            response.status === 'INSUFFICIENT_CONTEXT'
              ? { status: 'insufficient_context', sourceCount: 0 }
              : { ...response.grounding, sourceCount: response.sources.length },
          citations: response.sources,
        },
      ])
    } catch (error) {
      if (!mountedRef.current) return
      if (error?.name === 'AbortError') return
      if (error?.code === 'AUTH_REQUIRED' || error?.status === 401) {
        setSessionExpired(true)
        return
      }
      setMessages((current) => [
        ...current.filter((message) => message.id !== errorMessageId),
        {
          id: errorMessageId,
          role: 'assistant',
          isError: true,
          content: error.message,
          onRetry: () => sendMessage(content, { ...options, attachmentIds }, {
            userMessageId,
            errorMessageId,
          }),
        },
      ])
    } finally {
      if (mountedRef.current) {
        setLoading(false)
        abortControllerRef.current = null
      }
    }
  }

  return (
    <>
      {open ? (
        <div className={`tutor-panel-layer ${isLaunching ? 'tutor-panel-layer-launching' : ''}`.trim()}>
          <button
            className="tutor-backdrop"
            type="button"
            aria-label={`Đóng ${ASSISTANT_NAME}`}
            onClick={() => setOpen(false)}
          />
          {isGuest ? (
            <section
              id="tutor-dialog"
              className="tutor-panel tutor-panel-guest"
              role="dialog"
              aria-modal="true"
              aria-labelledby="tutor-dialog-title"
            >
              <header className="tutor-panel-header">
                <div>
                  <p className="progress-card-kicker">SẴN SÀNG HỖ TRỢ</p>
                  <h2 id="tutor-dialog-title" className="font-display">
                    {ASSISTANT_NAME}
                  </h2>
                </div>
                <button
                  className="tutor-close-button"
                  type="button"
                  aria-label={`Đóng ${ASSISTANT_NAME}`}
                  onClick={() => setOpen(false)}
                >
                  <X aria-hidden="true" size={19} />
                </button>
              </header>
              <div className="tutor-gate-body">
                <AuthGate forceGate={sessionExpired} />
              </div>
            </section>
          ) : (
            <TutorPanel
              state={shellState}
              onStateChange={setShellState}
              context={activeContext}
              onClearContext={activeContext?.skill !== 'GENERAL' ? handleClearContext : null}
              messages={messages}
              loading={loading}
              onClose={() => setOpen(false)}
              onCancel={handleCancel}
              onSend={sendMessage}
              inputRef={inputRef}
              attachment={attachment}
              attachments={attachmentQueue.attachments}
              onAttachmentSelected={handleAttachmentSelected}
              onAttachmentsSelected={handleAttachmentsSelected}
              onAttachmentError={handleAttachmentError}
              onRemoveAttachment={handleRemoveAttachment}
              onRetryAttachment={handleRetryAttachment}
              suggestions={proactiveSuggestionsEnabled ? undefined : []}
            />
          )}
        </div>
      ) : null}
      <AiTutorMascotLauncher buttonRef={buttonRef} open={open} onClick={handleOpenTutor} proactiveAiEnabled={proactiveSuggestionsEnabled} />
    </>
  )
}

export default FloatingTutor
