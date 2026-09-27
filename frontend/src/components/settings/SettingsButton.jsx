import { Settings2 } from 'lucide-react'
import { useOptionalPreferences } from '../../features/preferences/PreferenceProvider'

export default function SettingsButton({ openerRef, onClick, expanded }) {
  const preferenceContext = useOptionalPreferences()
  const label = preferenceContext?.translate?.('settings', 'Cài đặt') ?? 'Cài đặt'
  return (
    <button ref={openerRef} type="button" className="settings-trigger settings-trigger-utility" title={label} aria-label={label} aria-haspopup="dialog" aria-expanded={expanded} onClick={onClick}>
      <Settings2 aria-hidden="true" />
    </button>
  )
}
