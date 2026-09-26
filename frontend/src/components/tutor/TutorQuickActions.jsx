const defaultPromptSuggestions = [
  'Giải thích lỗi Writing của tôi',
  'Vì sao đáp án Reading này sai?',
  'Luyện Speaking Part 2',
]

export function TutorQuickActions({ onSelectPrompt, suggestions = defaultPromptSuggestions }) {
  if (!suggestions || suggestions.length === 0) return null

  return (
    <div className="tutor-prompt-list" aria-label="Gợi ý cho Trợ giảng AI">
      {suggestions.map((prompt) => (
        <button
          key={prompt}
          type="button"
          className="tutor-prompt-btn"
          onClick={() => onSelectPrompt(prompt)}
        >
          {prompt}
        </button>
      ))}
    </div>
  )
}

export default TutorQuickActions
