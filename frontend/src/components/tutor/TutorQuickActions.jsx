import { ASSISTANT_NAME } from '../../features/tutor/assistantIdentity'

const defaultPromptSuggestions = [
  'Giải thích lỗi Writing của tôi',
  'Vì sao đáp án Reading này sai?',
  'Luyện Speaking Part 2',
]

export function TutorQuickActions({ onSelectPrompt, suggestions = defaultPromptSuggestions }) {
  const validSuggestions = (suggestions || [])
    .map((suggestion) => {
      if (typeof suggestion === 'string') {
        const prompt = suggestion.trim()
        return prompt ? { label: prompt, prompt } : null
      }

      if (
        suggestion &&
        suggestion.valid !== false &&
        typeof suggestion.label === 'string' &&
        typeof suggestion.prompt === 'string' &&
        suggestion.label.trim() &&
        suggestion.prompt.trim()
      ) {
        return {
          label: suggestion.label.trim(),
          prompt: suggestion.prompt.trim(),
        }
      }

      return null
    })
    .filter(Boolean)

  if (validSuggestions.length === 0) return null

  return (
    <div className="tutor-prompt-list" aria-label={`Gợi ý cho ${ASSISTANT_NAME}`}>
      {validSuggestions.map(({ label, prompt }) => (
        <button
          key={prompt}
          type="button"
          className="tutor-prompt-btn"
          onClick={() => onSelectPrompt(prompt)}
        >
          {label}
        </button>
      ))}
    </div>
  )
}

export default TutorQuickActions
