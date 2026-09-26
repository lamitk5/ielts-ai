import { useContext, useEffect, useRef, useState } from 'react'
import { UNSAFE_LocationContext } from 'react-router-dom'
import { X } from 'lucide-react'
import FloatingTutorButton from './FloatingTutorButton'
import TutorPanel from './TutorPanel'
import { SHELL_STATES } from './TutorShell'
import AuthGate from '../auth/AuthGate'
import { useOptionalAuth } from '../../features/auth/AuthProvider'
import { MAX_HISTORY_MESSAGES, sendTutorMessage } from '../../services/aiTutorApi'

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
  const location = useSafeLocation()
  const [open, setOpen] = useState(false)
  const [shellState, setShellState] = useState(SHELL_STATES.STANDARD)
  const [messages, setMessages] = useState([welcomeMessage])
  const [loading, setLoading] = useState(false)
  const [sessionExpired, setSessionExpired] = useState(false)
  const [activeContext, setActiveContext] = useState(context)
  const buttonRef = useRef(null)
  const inputRef = useRef(null)
  const openedRef = useRef(false)
  const mountedRef = useRef(true)
  const abortControllerRef = useRef(null)
  const initialPathRef = useRef(location?.pathname ?? '/')

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
    inputRef.current?.focus()

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        event.preventDefault()
        setOpen(false)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [open])

  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
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

  async function sendMessage(content) {
    if (loading) return
    const history = messages.slice(-MAX_HISTORY_MESSAGES).map((message) => ({
      role: message.role === 'user' ? 'USER' : 'ASSISTANT',
      content: message.content,
    }))
    setMessages((current) => [
      ...current,
      { id: `user-${Date.now()}`, role: 'user', content },
    ])
    setLoading(true)

    const controller = new AbortController()
    abortControllerRef.current = controller

    try {
      const response = await sendTutorMessage(
        { message: content, context: activeContext, history },
        { signal: controller.signal },
      )
      if (!mountedRef.current) return
      setMessages((current) => [
        ...current,
        {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          status: response.status,
          content: response.answer,
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
        ...current,
        {
          id: `assistant-error-${Date.now()}`,
          role: 'assistant',
          isError: true,
          content: error.message,
          onRetry: () => sendMessage(content),
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
        <div className="tutor-panel-layer">
          <button
            className="tutor-backdrop"
            type="button"
            aria-label="Đóng Trợ giảng AI"
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
                    Trợ giảng AI
                  </h2>
                </div>
                <button
                  className="tutor-close-button"
                  type="button"
                  aria-label="Đóng Trợ giảng AI"
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
            />
          )}
        </div>
      ) : null}
      <FloatingTutorButton buttonRef={buttonRef} open={open} onClick={() => setOpen(true)} />
    </>
  )
}

export default FloatingTutor
