import { Eye, EyeOff } from 'lucide-react'
import { useId, useState } from 'react'

function PasswordFlashlightField({ id, label, value, onChange, autoComplete, minLength, visibilityLabel, showLabel = 'Hiện', hideLabel = 'Ẩn', disabled = false }) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const [isVisible, setIsVisible] = useState(false)
  const toggleLabel = visibilityLabel ?? label.toLowerCase()

  return (
    <div className={`password-flashlight-field ${isVisible ? 'password-flashlight-on' : ''}`.trim()}>
      <label htmlFor={inputId}>{label}</label>
      <div className="password-flashlight-control">
        <input
          id={inputId}
          type={isVisible ? 'text' : 'password'}
          autoComplete={autoComplete}
          minLength={minLength}
          value={value}
          onChange={onChange}
          disabled={disabled}
          required
        />
        <span className="password-flashlight-beam" data-testid="password-flashlight-beam" data-active={isVisible} aria-hidden="true" />
        <button
          type="button"
          className="password-visibility-toggle"
          aria-label={`${isVisible ? hideLabel : showLabel} ${toggleLabel}`}
          aria-pressed={isVisible}
          disabled={disabled}
          onClick={() => setIsVisible((visible) => !visible)}
        >
          {isVisible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
        </button>
      </div>
    </div>
  )
}

export default PasswordFlashlightField
