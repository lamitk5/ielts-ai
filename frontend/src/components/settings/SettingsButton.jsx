import { Settings2 } from 'lucide-react'

export default function SettingsButton({ openerRef, onClick, expanded }) {
  return (
    <button ref={openerRef} type="button" className="settings-trigger" aria-label="Cài đặt" aria-haspopup="dialog" aria-expanded={expanded} onClick={onClick}>
      <Settings2 aria-hidden="true" />
      <span>Cài đặt</span>
    </button>
  )
}
