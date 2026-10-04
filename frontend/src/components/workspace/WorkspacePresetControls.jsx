import { WORKSPACE_RATIO_PRESETS } from '../../features/preferences/preferenceSchema'

export function WorkspacePresetControls({ value = 40, onChange }) {
  return (
    <div className="workspace-preset-controls" role="group" aria-label="Tỷ lệ khung học tập">
      {WORKSPACE_RATIO_PRESETS.map((ratio) => (
        <button
          key={ratio}
          type="button"
          aria-pressed={value === ratio}
          onClick={() => onChange?.(ratio)}
        >
          {ratio}/{100 - ratio}
        </button>
      ))}
    </div>
  )
}
