const controls = [
  { key: 'themeMode', label: 'Giao diện', options: [['system', 'Theo hệ thống'], ['light', 'Sáng'], ['dark', 'Tối']] },
  { key: 'accentPreset', label: 'Màu nhấn', options: [['gold', 'Gold'], ['sapphire', 'Sapphire'], ['emerald', 'Emerald'], ['burgundy', 'Burgundy'], ['violet', 'Violet'], ['slate', 'Slate']] },
  { key: 'fontScale', label: 'Cỡ chữ', options: [['small', 'Nhỏ'], ['default', 'Mặc định'], ['large', 'Lớn']] },
  { key: 'density', label: 'Mật độ', options: [['spacious', 'Thoáng'], ['default', 'Mặc định'], ['compact', 'Gọn']] },
  { key: 'reduceMotion', label: 'Chuyển động', options: [['system', 'Theo hệ thống'], ['reduce', 'Giảm chuyển động'], ['allow', 'Cho phép chuyển động']] },
]

const learning = [
  { key: 'proactiveAiEnabled', label: 'Gợi ý từ Trợ giảng AI' },
  { key: 'crossHighlightEnabled', label: 'Đánh dấu liên kết' },
  { key: 'timerDefaultEnabled', label: 'Hiện đồng hồ luyện tập' },
]

export default function PreferenceControlGroup({ preferences, updatePreference }) {
  const preview = controls.map(({ key, label, options }) =>
    `${label}: ${options.find(([value]) => value === preferences[key])?.[1]}`,
  ).join(' · ')
  return (
    <>
      <p className="settings-preview" aria-live="polite">Xem trước: {preview}</p>
      <fieldset className="settings-group">
        <legend>Giao diện</legend>
        {controls.map(({ key, label, options }) => (
          <label className="settings-field" key={key}>
            <span>{label}</span>
            <select value={preferences[key]} onChange={(event) => updatePreference(key, event.target.value)}>
              {options.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
            </select>
          </label>
        ))}
      </fieldset>
      <fieldset className="settings-group">
        <legend>Học tập</legend>
        {learning.map(({ key, label }) => (
          <label className="settings-switch" key={key}>
            <span>{label}</span>
            <input type="checkbox" checked={preferences[key]} onChange={(event) => updatePreference(key, event.target.checked)} />
          </label>
        ))}
        {[
          ['readingSplitRatio', 'Tỷ lệ khung Reading'],
          ['writingSplitRatio', 'Tỷ lệ khung Writing'],
        ].map(([key, label]) => (
          <label className="settings-field" key={key}>
            <span>{label}</span>
            <select value={preferences[key]} onChange={(event) => updatePreference(key, Number(event.target.value))}>
              {[40, 50, 60].map((ratio) => <option value={ratio} key={ratio}>{ratio}%</option>)}
            </select>
          </label>
        ))}
      </fieldset>
    </>
  )
}
