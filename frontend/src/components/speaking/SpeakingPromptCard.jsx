import GlassCard from '../common/GlassCard'

export function SpeakingPromptCard({
  prompts = [],
  selectedPromptId,
  onSelectPrompt,
}) {
  const prompt = prompts.find((item) => item.id === selectedPromptId) ?? prompts[0]

  if (!prompt) return null

  return (
    <GlassCard className="speaking-prompt-card">
      <div className="speaking-prompt-toolbar">
        <label htmlFor="speaking-part-select" className="speaking-part-select-label">
          Phần thi
        </label>
        <select
          id="speaking-part-select"
          className="speaking-part-select"
          value={selectedPromptId}
          onChange={(event) => onSelectPrompt?.(event.target.value)}
        >
          {prompts.map((item) => (
            <option key={item.id} value={item.id}>
              {item.part} · {item.text.slice(0, 32)}…
            </option>
          ))}
        </select>
      </div>

      <div className="speaking-prompt-header">
        <span className="speaking-part-badge">{prompt.part}</span>
        <h2 className="speaking-prompt-text font-display">{prompt.text}</h2>
      </div>

      {Array.isArray(prompt.points) && prompt.points.length > 0 ? (
        <div className="speaking-prompt-points">
          <p className="speaking-points-title">Gợi ý nội dung (Cue Card):</p>
          <ul className="speaking-points-list">
            {prompt.points.map((point, index) => (
              <li key={index}>{point}</li>
            ))}
          </ul>
        </div>
      ) : null}
    </GlassCard>
  )
}

export default SpeakingPromptCard
