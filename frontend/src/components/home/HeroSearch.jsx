import { Search } from 'lucide-react'
import { useState } from 'react'
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
  const [query, setQuery] = useState('')
  const [error, setError] = useState('')

  function submitSearch(event) {
    event.preventDefault()
    const trimmedQuery = query.trim()

    if (!trimmedQuery) {
      setError('Nhập nội dung bạn muốn luyện tập.')
      return
    }

    setError('')
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
        <div className="hero-search-control">
          <Search aria-hidden="true" className="hero-search-icon" size={19} />
          <input
            id="hero-search-input"
            aria-describedby={error ? 'hero-search-error' : undefined}
            aria-invalid={Boolean(error)}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value)
              if (error) setError('')
            }}
            placeholder="IELTS Writing Task 1 Line Graph"
          />
          <button className="hero-search-submit" type="submit">
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
