import { useEffect, useRef, useState } from 'react'
import FloatingTutorButton from './FloatingTutorButton'
import TutorPanel from './TutorPanel'
import { tutorGroundedDemo, tutorInsufficientDemo } from '../../data/homepageMockData'

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
  const timeoutRef = useRef(null)
  const openedRef = useRef(false)

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

  useEffect(() => () => clearTimeout(timeoutRef.current), [])

  function sendMessage(content) {
    setMessages((current) => [
      ...current,
      { id: `user-${Date.now()}`, role: 'user', content },
    ])
    setLoading(true)
    clearTimeout(timeoutRef.current)

    timeoutRef.current = setTimeout(() => {
      const response = content.toLowerCase().includes('ngữ cảnh')
        ? tutorInsufficientDemo
        : tutorGroundedDemo

      setMessages((current) => [
        ...current,
        { id: `assistant-${Date.now()}`, role: 'assistant', ...response },
      ])
      setLoading(false)
    }, 300)
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
