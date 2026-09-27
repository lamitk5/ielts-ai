const themes = [
  ['dark', 'Tối'],
  ['light', 'Sáng'],
  ['system', 'Theo hệ thống'],
]

const accents = [
  ['gold', 'Gold'],
  ['sapphire', 'Sapphire'],
  ['emerald', 'Emerald'],
  ['burgundy', 'Burgundy'],
  ['violet', 'Violet'],
  ['slate', 'Slate'],
]

const selectOptions = {
  fontScale: [['small', 'Nhỏ'], ['default', 'Mặc định'], ['large', 'Lớn']],
  density: [['spacious', 'Thoáng'], ['default', 'Tiêu chuẩn'], ['compact', 'Gọn']],
}

function SelectField({ label, value, options, onChange, numeric = false }) {
  return (
    <label className="settings-field">
      <span>{label}</span>
      <select aria-label={label} value={value} onChange={(event) => onChange(numeric ? Number(event.target.value) : event.target.value)}>
        {options.map(([optionValue, text]) => <option key={optionValue} value={optionValue}>{text}</option>)}
      </select>
    </label>
  )
}

function PreferenceControlGroup({ preferences, updatePreference }) {
  const preview = [
    ['Giao diện', themes.find(([value]) => value === preferences.themeMode)?.[1]],
    ['Màu nhấn', accents.find(([value]) => value === preferences.accentPreset)?.[1]],
    ['Cỡ chữ', selectOptions.fontScale.find(([value]) => value === preferences.fontScale)?.[1]],
    ['Mật độ hiển thị', selectOptions.density.find(([value]) => value === preferences.density)?.[1]],
  ].map(([label, value]) => `${label}: ${value}`).join(' · ')

  const fontScaleValue = { small: 0, default: 1, large: 2 }[preferences.fontScale]

  return (
    <>
      <p className="settings-preview" aria-live="polite">Xem trước: {preview}</p>

      <fieldset className="settings-group settings-section">
        <legend><span role="heading" aria-level="3">Giao diện &amp; Hiển thị</span></legend>
        <div className="settings-theme-previews" aria-label="Xem trước giao diện">
          {themes.map(([value, label]) => (
            <button type="button" key={value} className={`settings-theme-preview ${preferences.themeMode === value ? 'settings-theme-preview-active' : ''}`.trim()} aria-pressed={preferences.themeMode === value} onClick={() => updatePreference('themeMode', value)}>
              <span className={`settings-theme-preview-swatch settings-theme-preview-${value}`} aria-hidden="true" />
              <span>{label}</span>
            </button>
          ))}
        </div>
        <SelectField label="Giao diện" value={preferences.themeMode} options={themes} onChange={(value) => updatePreference('themeMode', value)} />
        <SelectField label="Màu nhấn" value={preferences.accentPreset} options={accents} onChange={(value) => updatePreference('accentPreset', value)} />
        <div className="settings-accent-swatches" aria-label="Các màu nhấn">
          {accents.map(([value, label]) => (
            <button type="button" key={value} className={`settings-accent-swatch settings-accent-${value}`.trim()} aria-label={`Chọn màu nhấn ${label}`} aria-pressed={preferences.accentPreset === value} onClick={() => updatePreference('accentPreset', value)}>
              <span className="sr-only">{label}</span>
            </button>
          ))}
        </div>
        <SelectField label="Mật độ hiển thị" value={preferences.density} options={selectOptions.density} onChange={(value) => updatePreference('density', value)} />
      </fieldset>

      <fieldset className="settings-group settings-section">
        <legend><span role="heading" aria-level="3">Chuyển động &amp; Trợ năng</span></legend>
        <label className="settings-switch">
          <span>Cho phép hiệu ứng giao diện (Animation)</span>
          <input aria-label="Cho phép hiệu ứng giao diện (Animation)" type="checkbox" checked={preferences.reduceMotion !== 'reduce'} onChange={(event) => updatePreference('reduceMotion', event.target.checked ? 'allow' : 'reduce')} />
        </label>
        <label className="settings-field settings-range-field">
          <span>Cỡ chữ</span>
          <input aria-label="Cỡ chữ" type="range" min="0" max="2" step="1" value={fontScaleValue} onChange={(event) => updatePreference('fontScale', ['small', 'default', 'large'][Number(event.target.value)])} />
        </label>
        <label className="settings-field">
          <span>Ngôn ngữ</span>
          <select aria-label="Ngôn ngữ" value={preferences.language} onChange={(event) => updatePreference('language', event.target.value)}>
            <option value="vi">Tiếng Việt</option>
            <option value="en">English</option>
          </select>
        </label>
      </fieldset>

      <fieldset className="settings-group settings-section">
        <legend><span role="heading" aria-level="3">Không gian học tập &amp; AI Tutor</span></legend>
        <label className="settings-switch">
          <span>AI gợi ý chủ động</span>
          <input aria-label="AI gợi ý chủ động" type="checkbox" checked={preferences.proactiveAiEnabled} onChange={(event) => updatePreference('proactiveAiEnabled', event.target.checked)} />
        </label>
        <label className="settings-switch">
          <span>Bật hiệu ứng sáng vùng lỗi sai (Cross-highlighting)</span>
          <input aria-label="Bật hiệu ứng sáng vùng lỗi sai (Cross-highlighting)" type="checkbox" checked={preferences.crossHighlightEnabled} onChange={(event) => updatePreference('crossHighlightEnabled', event.target.checked)} />
        </label>
        <label className="settings-switch">
          <span>Hiển thị đồng hồ đếm ngược</span>
          <input aria-label="Hiển thị đồng hồ đếm ngược" type="checkbox" checked={preferences.timerDefaultEnabled} onChange={(event) => updatePreference('timerDefaultEnabled', event.target.checked)} />
        </label>
        <SelectField label="Tỷ lệ chia Reading" value={preferences.readingSplitRatio} options={[[40, '40 / 60'], [50, '50 / 50'], [60, '60 / 40']]} numeric onChange={(value) => updatePreference('readingSplitRatio', value)} />
        <SelectField label="Tỷ lệ chia Writing" value={preferences.writingSplitRatio} options={[[40, '40 / 60'], [50, '50 / 50'], [60, '60 / 40']]} numeric onChange={(value) => updatePreference('writingSplitRatio', value)} />
      </fieldset>
    </>
  )
}

export default PreferenceControlGroup
