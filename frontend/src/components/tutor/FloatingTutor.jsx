import { useEffect, useRef, useState } from 'react'
import FloatingTutorButton from './FloatingTutorButton'
import TutorPanel from './TutorPanel'
import { MAX_HISTORY_MESSAGES, sendTutorMessage } from '../../services/aiTutorApi'

const welcomeMessage = {
  id: 'welcome',
  role: 'assistant',
  content: 'Mình có thể giúp bạn hiểu bài, xem lại lỗi và chọn bước luyện tập tiếp theo.',
}

function FloatingTutor() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState([welcomeMessage])
  const [loading, setLoading] = useState(false)
  const buttonRef = useRef(null)
  const inputRef = useRef(null)
  const openedRef = useRef(false)
  const mountedRef = useRef(true)

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

  useEffect(() => () => {
    mountedRef.current = false
  }, [])

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
    try {
      const response = await sendTutorMessage({ message: content, context: { skill: 'GENERAL' }, history })
      if (!mountedRef.current) return
      setMessages((current) => [
        ...current,
        {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          status: response.status,
          content: response.answer,
          grounding: response.status === 'INSUFFICIENT_CONTEXT'
            ? { status: 'insufficient_context', sourceCount: 0 }
            : response.grounding,
          citations: response.sources,
        },
      ])
    } catch (error) {
      if (!mountedRef.current) return
      setMessages((current) => [
        ...current,
        { id: `assistant-error-${Date.now()}`, role: 'assistant', isError: true, content: error.message },
      ])
    } finally {
      if (mountedRef.current) setLoading(false)
    }
  }

  return (
    <>
      {open ? (
        <div className="tutor-panel-layer">
          <button className="tutor-backdrop" type="button" aria-label="Đóng Trợ giảng AI" onClick={() => setOpen(false)} />
          <TutorPanel
            messages={messages}
            loading={loading}
            onClose={() => setOpen(false)}
            onSend={sendMessage}
            inputRef={inputRef}
          />
        </div>
      ) : null}
      <FloatingTutorButton buttonRef={buttonRef} open={open} onClick={() => setOpen(true)} />
    </>
  )
}

export default FloatingTutor
