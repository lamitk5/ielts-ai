import { Sparkles } from 'lucide-react'

function FloatingTutorButton({ onClick, buttonRef, open }) {
  return (
    <button
      ref={buttonRef}
      className="floating-tutor-button"
      type="button"
      aria-label="Mở Trợ giảng AI"
      aria-expanded={open}
      aria-controls="tutor-dialog"
      onClick={onClick}
    >
      <Sparkles aria-hidden="true" size={19} />
      <span>Trợ giảng AI</span>
    </button>
  )
}

export default FloatingTutorButton
