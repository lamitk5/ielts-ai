import { Search } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'

const suggestions = [
  { label: 'Reading', to: '/practice/reading' },
  { label: 'Listening', to: '/practice/listening' },
  { label: 'Writing Task 2', query: 'Writing Task 2' },
  { label: 'Speaking Part 2', query: 'Speaking Part 2' },
]

function HeroSuggestions() {
  const navigate = useNavigate()

  function selectSuggestion(suggestion) {
    if (suggestion.to) {
      navigate(suggestion.to)
      return
    }

    navigate(`/practice/search?q=${encodeURIComponent(suggestion.query)}`)
  }

  return (
    <div className="hero-suggestions" aria-labelledby="hero-suggestions-label">
      <span id="hero-suggestions-label" className="hero-suggestions-label">
        Gợi ý nhanh
      </span>
      <div className="hero-suggestion-list">
        {suggestions.map((suggestion) => (
          <button
            key={suggestion.label}
            className="hero-suggestion-chip"
            type="button"
            onClick={() => selectSuggestion(suggestion)}
          >
            {suggestion.label}
          </button>
        ))}
      </div>
    </div>
  )
}

function HeroSearch() {
  const navigate = useNavigate()
  const controlRef = useRef(null)
  const submitTimerRef = useRef(null)
  const [query, setQuery] = useState('')
  const [error, setError] = useState('')
  const [isFocused, setIsFocused] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => () => {
    if (submitTimerRef.current) window.clearTimeout(submitTimerRef.current)
  }, [])

  function handlePointerMove(event) {
    if (event.pointerType && event.pointerType !== 'mouse' && event.pointerType !== 'pen') return

    const control = controlRef.current
    if (!control) return

    const rect = control.getBoundingClientRect()
    const x = Math.max(0, Math.min(rect.width, event.clientX - rect.left))
    const y = Math.max(0, Math.min(rect.height, event.clientY - rect.top))

    control.style.setProperty('--search-pointer-x', `${x}px`)
    control.style.setProperty('--search-pointer-y', `${y}px`)
    control.dataset.pointerActive = 'true'
  }

  function resetPointer() {
    const control = controlRef.current
    if (!control) return

    control.style.setProperty('--search-pointer-x', '50%')
    control.style.setProperty('--search-pointer-y', '50%')
    delete control.dataset.pointerActive
  }

  function submitSearch(event) {
    event.preventDefault()
    const trimmedQuery = query.trim()

    if (!trimmedQuery) {
      setError('Nhập nội dung bạn muốn luyện tập.')
      return
    }

    setError('')
    setIsSubmitting(true)
    submitTimerRef.current = window.setTimeout(() => {
      setIsSubmitting(false)
      submitTimerRef.current = null
    }, 360)
    navigate(`/practice/search?q=${encodeURIComponent(trimmedQuery)}`)
  }

  return (
    <div className="hero-search-wrap">
      <form
        className="hero-search"
        role="search"
        aria-label="Tìm nhanh bài luyện IELTS"
        onSubmit={submitSearch}
      >
        <label className="sr-only" htmlFor="hero-search-input">
          Tìm nội dung luyện tập IELTS
        </label>
        <div
          ref={controlRef}
          className={`hero-search-control${isFocused ? ' hero-search-control-focused' : ''}${isSubmitting ? ' hero-search-control-submitting' : ''}`}
          data-search-focused={isFocused ? 'true' : undefined}
          onPointerLeave={resetPointer}
          onPointerMove={handlePointerMove}
          style={{ '--search-pointer-x': '50%', '--search-pointer-y': '50%' }}
        >
          <Search aria-hidden="true" className="hero-search-icon" size={19} />
          <input
            id="hero-search-input"
            aria-describedby={error ? 'hero-search-error' : undefined}
            aria-invalid={Boolean(error)}
            value={query}
            onBlur={() => setIsFocused(false)}
            onChange={(event) => {
              setQuery(event.target.value)
              if (error) setError('')
            }}
            onFocus={() => setIsFocused(true)}
            placeholder="IELTS Writing Task 1 Line Graph"
          />
          <button className="hero-search-submit button-interactive" type="submit">
            <span>Tìm bài luyện</span>
            <Search aria-hidden="true" size={16} />
          </button>
        </div>
        {error ? (
          <p id="hero-search-error" className="hero-search-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>
    </div>
  )
}

export { HeroSuggestions }
export default HeroSearch
