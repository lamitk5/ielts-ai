import { useOptionalPreferences } from '../../features/preferences/PreferenceProvider'

const themes = [
  ['dark', 'themeDark', 'Tối'],
  ['light', 'themeLight', 'Sáng'],
  ['system', 'themeSystem', 'Theo hệ thống'],
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
  fontScale: [['small', 'fontSmall', 'Nhỏ'], ['default', 'fontDefault', 'Mặc định'], ['large', 'fontLarge', 'Lớn']],
  density: [['spacious', 'densitySpacious', 'Thoáng'], ['default', 'densityDefault', 'Tiêu chuẩn'], ['compact', 'densityCompact', 'Gọn']],
}

const cursorStyles = [
  ['default', 'cursorDefault', 'Mặc định'],
  ['champagne', 'cursorChampagne', 'Champagne Gold'],
  ['scholar-pen', 'cursorScholarPen', 'Scholar Pen'],
  ['en-feather', 'cursorEnFeather', 'Én Feather'],
  ['pixel-scholar', 'cursorPixelScholar', 'Pixel Scholar'],
]

function SelectField({ label, value, options, onChange, numeric = false, translate }) {
  return (
    <label className="settings-field">
      <span>{label}</span>
      <select aria-label={label} value={value} onChange={(event) => onChange(numeric ? Number(event.target.value) : event.target.value)}>
        {options.map(([optionValue, textOrKey, fallback]) => <option key={optionValue} value={optionValue}>{translate ? translate(textOrKey, fallback ?? textOrKey) : fallback ?? textOrKey}</option>)}
      </select>
    </label>
  )
}

function PreferenceControlGroup({ preferences, updatePreference }) {
  const preferenceContext = useOptionalPreferences()
  const translate = preferenceContext?.translate ?? ((_key, fallback) => fallback)
  const preview = [
    [translate('theme', 'Giao diện'), translate(themes.find(([value]) => value === preferences.themeMode)?.[1], themes.find(([value]) => value === preferences.themeMode)?.[2])],
    [translate('accent', 'Màu nhấn'), accents.find(([value]) => value === preferences.accentPreset)?.[1]],
    [translate('fontSize', 'Cỡ chữ'), translate(selectOptions.fontScale.find(([value]) => value === preferences.fontScale)?.[1], selectOptions.fontScale.find(([value]) => value === preferences.fontScale)?.[2])],
    [translate('density', 'Mật độ hiển thị'), translate(selectOptions.density.find(([value]) => value === preferences.density)?.[1], selectOptions.density.find(([value]) => value === preferences.density)?.[2])],
  ].map(([label, value]) => `${label}: ${value}`).join(' · ')

  const fontScaleValue = { small: 0, default: 1, large: 2 }[preferences.fontScale]
  const updateFontScale = (nextValue) => updatePreference('fontScale', ['small', 'default', 'large'][Math.max(0, Math.min(2, nextValue))])

  return (
    <>
      <p className="settings-preview" aria-live="polite">{translate('preview', 'Xem trước')}: {preview}</p>

      <fieldset className="settings-group settings-section">
        <legend><span role="heading" aria-level="3">{translate('appearance', 'Giao diện & Hiển thị')}</span></legend>
        <div className="settings-theme-previews" aria-label={translate('themePreview', 'Xem trước giao diện')}>
          {themes.map(([value, labelKey, fallback]) => (
            <button type="button" key={value} className={`settings-theme-preview ${preferences.themeMode === value ? 'settings-theme-preview-active' : ''}`.trim()} aria-pressed={preferences.themeMode === value} onClick={() => updatePreference('themeMode', value)}>
              <span className={`settings-theme-preview-swatch settings-theme-preview-${value}`} aria-hidden="true" />
              <span>{translate(labelKey, fallback)}</span>
            </button>
          ))}
        </div>
        <SelectField label={translate('theme', 'Giao diện')} value={preferences.themeMode} options={themes} translate={translate} onChange={(value) => updatePreference('themeMode', value)} />
        <SelectField label={translate('accent', 'Màu nhấn')} value={preferences.accentPreset} options={accents} translate={translate} onChange={(value) => updatePreference('accentPreset', value)} />
        <div className="settings-accent-swatches" aria-label={translate('accentSwatches', 'Các màu nhấn')}>
          {accents.map(([value, label]) => (
            <button type="button" key={value} className={`settings-accent-swatch settings-accent-${value}`.trim()} aria-label={`${translate('accentChoose', 'Chọn màu nhấn')} ${label}`} aria-pressed={preferences.accentPreset === value} onClick={() => updatePreference('accentPreset', value)}>
              <span className="sr-only">{label}</span>
            </button>
          ))}
        </div>
        <SelectField label={translate('density', 'Mật độ hiển thị')} value={preferences.density} options={selectOptions.density} translate={translate} onChange={(value) => updatePreference('density', value)} />
        <div className="settings-cursor-field">
          <span className="settings-field-label">{translate('cursorStyle', 'Kiểu con trỏ')}</span>
          <div className="settings-cursor-options" aria-label={translate('cursorStyleOptions', 'Các kiểu con trỏ')}>
            {cursorStyles.map(([value, labelKey, fallback]) => (
              <button
                type="button"
                key={value}
                className={`settings-cursor-option ${preferences.cursorStyle === value ? 'settings-cursor-option-active' : ''}`.trim()}
                aria-pressed={preferences.cursorStyle === value}
                aria-label={translate(labelKey, fallback)}
                onClick={() => updatePreference('cursorStyle', value)}
              >
                <span className={`settings-cursor-preview settings-cursor-preview-${value}`} aria-hidden="true" />
                <span>{translate(labelKey, fallback)}</span>
                {preferences.cursorStyle === value ? <span className="settings-cursor-check" aria-hidden="true">✓</span> : null}
              </button>
            ))}
          </div>
          <label className="settings-switch">
            <span>{translate('cursorEffects', 'Hiệu ứng con trỏ')}</span>
            <input aria-label={translate('cursorEffects', 'Hiệu ứng con trỏ')} type="checkbox" checked={preferences.cursorEffects} onChange={(event) => updatePreference('cursorEffects', event.target.checked)} />
          </label>
        </div>
      </fieldset>

      <fieldset className="settings-group settings-section">
        <legend><span role="heading" aria-level="3">{translate('motion', 'Chuyển động & Trợ năng')}</span></legend>
        <label className="settings-switch">
          <span>{translate('animation', 'Cho phép hiệu ứng giao diện (Animation)')}</span>
          <input aria-label={translate('animation', 'Cho phép hiệu ứng giao diện (Animation)')} type="checkbox" checked={preferences.reduceMotion !== 'reduce'} onChange={(event) => updatePreference('reduceMotion', event.target.checked ? 'allow' : 'reduce')} />
        </label>
        <label className="settings-field settings-range-field">
          <span>{translate('fontSize', 'Cỡ chữ')}</span>
          <div className="settings-range-control">
            <button type="button" aria-label={translate('fontDecrease', 'A-')} onClick={() => updateFontScale(fontScaleValue - 1)}>{translate('fontDecrease', 'A-')}</button>
            <input aria-label={translate('fontSize', 'Cỡ chữ')} type="range" min="0" max="2" step="1" value={fontScaleValue} onChange={(event) => updateFontScale(Number(event.target.value))} />
            <button type="button" aria-label={translate('fontIncrease', 'A+')} onClick={() => updateFontScale(fontScaleValue + 1)}>{translate('fontIncrease', 'A+')}</button>
          </div>
        </label>
        <label className="settings-field">
          <span>{translate('language', 'Ngôn ngữ')}</span>
          <select aria-label={translate('language', 'Ngôn ngữ')} value={preferences.language} onChange={(event) => updatePreference('language', event.target.value)}>
            <option value="vi">{translate('vietnamese', 'Tiếng Việt')}</option>
            <option value="en">{translate('english', 'English')}</option>
          </select>
        </label>
      </fieldset>

      <fieldset className="settings-group settings-section">
        <legend><span role="heading" aria-level="3">{translate('learning', 'Không gian học tập & Én')}</span></legend>
        <label className="settings-switch">
          <span>{translate('proactive', 'Bật gợi ý chủ động từ Én')}</span>
          <input aria-label={translate('proactive', 'Bật gợi ý chủ động từ Én')} type="checkbox" checked={preferences.proactiveAiEnabled} onChange={(event) => updatePreference('proactiveAiEnabled', event.target.checked)} />
        </label>
        <label className="settings-switch">
          <span>{translate('crossHighlight', 'Bật hiệu ứng sáng vùng lỗi sai (Cross-highlighting)')}</span>
          <input aria-label={translate('crossHighlight', 'Bật hiệu ứng sáng vùng lỗi sai (Cross-highlighting)')} type="checkbox" checked={preferences.crossHighlightEnabled} onChange={(event) => updatePreference('crossHighlightEnabled', event.target.checked)} />
        </label>
        <label className="settings-switch">
          <span>{translate('countdown', 'Hiển thị đồng hồ đếm ngược')}</span>
          <input aria-label={translate('countdown', 'Hiển thị đồng hồ đếm ngược')} type="checkbox" checked={preferences.timerDefaultEnabled} onChange={(event) => updatePreference('timerDefaultEnabled', event.target.checked)} />
        </label>
        <SelectField label={translate('readingSplit', 'Tỷ lệ chia Reading')} value={preferences.readingSplitRatio} options={[[40, '40 / 60'], [50, '50 / 50'], [60, '60 / 40']]} translate={translate} numeric onChange={(value) => updatePreference('readingSplitRatio', value)} />
        <SelectField label={translate('writingSplit', 'Tỷ lệ chia Writing')} value={preferences.writingSplitRatio} options={[[40, '40 / 60'], [50, '50 / 50'], [60, '60 / 40']]} translate={translate} numeric onChange={(value) => updatePreference('writingSplitRatio', value)} />
      </fieldset>
    </>
  )
}

export default PreferenceControlGroup
