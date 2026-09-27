const themes = [
  ['system', 'Theo hệ thống'],
  ['light', 'Sáng'],
  ['dark', 'Tối'],
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
  reduceMotion: [['system', 'Theo hệ thống'], ['reduce', 'Giảm chuyển động'], ['allow', 'Cho phép chuyển động']],
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
    ['Chuyển động', selectOptions.reduceMotion.find(([value]) => value === preferences.reduceMotion)?.[1]],
  ].map(([label, value]) => `${label}: ${value}`).join(' · ')

  return (
    <>
      <p className="settings-preview" aria-live="polite">Xem trước: {preview}</p>

      <fieldset className="settings-group">
        <legend>Giao diện</legend>
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
      </fieldset>

      <fieldset className="settings-group">
        <legend>Ngôn ngữ</legend>
        <label className="settings-field">
          <span>Ngôn ngữ giao diện</span>
          <select aria-label="Ngôn ngữ" value="vi" disabled onChange={() => {}}><option value="vi">Tiếng Việt</option></select>
        </label>
      </fieldset>

      <fieldset className="settings-group">
        <legend>Phông chữ</legend>
        <SelectField label="Cỡ chữ" value={preferences.fontScale} options={selectOptions.fontScale} onChange={(value) => updatePreference('fontScale', value)} />
      </fieldset>

      <fieldset className="settings-group">
        <legend>Mật độ hiển thị</legend>
        <SelectField label="Mật độ hiển thị" value={preferences.density} options={selectOptions.density} onChange={(value) => updatePreference('density', value)} />
      </fieldset>

      <fieldset className="settings-group">
        <legend>Chuyển động</legend>
        <SelectField label="Chuyển động" value={preferences.reduceMotion} options={selectOptions.reduceMotion} onChange={(value) => updatePreference('reduceMotion', value)} />
      </fieldset>

      <fieldset className="settings-group">
        <legend>Trợ giảng AI</legend>
        <label className="settings-switch">
          <span>AI gợi ý chủ động</span>
          <input aria-label="AI gợi ý chủ động" type="checkbox" checked={preferences.proactiveAiEnabled} onChange={(event) => updatePreference('proactiveAiEnabled', event.target.checked)} />
        </label>
      </fieldset>

      <fieldset className="settings-group">
        <legend>Quyền riêng tư</legend>
        <p className="settings-group-copy">Bạn luôn kiểm soát những tín hiệu học tập được hiển thị trong phiên luyện tập.</p>
        <label className="settings-switch">
          <span>Đánh dấu liên kết</span>
          <input type="checkbox" checked={preferences.crossHighlightEnabled} onChange={(event) => updatePreference('crossHighlightEnabled', event.target.checked)} />
        </label>
      </fieldset>

      <fieldset className="settings-group">
        <legend>Luyện tập</legend>
        <label className="settings-switch">
          <span>Hiện đồng hồ luyện tập</span>
          <input type="checkbox" checked={preferences.timerDefaultEnabled} onChange={(event) => updatePreference('timerDefaultEnabled', event.target.checked)} />
        </label>
        <SelectField label="Tỷ lệ chia Reading" value={preferences.readingSplitRatio} options={[[40, '40 / 60'], [50, '50 / 50']]} numeric onChange={(value) => updatePreference('readingSplitRatio', value)} />
        <SelectField label="Tỷ lệ chia Writing" value={preferences.writingSplitRatio} options={[[40, '40 / 60'], [50, '50 / 50']]} numeric onChange={(value) => updatePreference('writingSplitRatio', value)} />
      </fieldset>
    </>
  )
}

export default PreferenceControlGroup
