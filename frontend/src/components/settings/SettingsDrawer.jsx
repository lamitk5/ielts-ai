import { useEffectEvent, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import Button from '../common/Button'
import { usePreferences } from '../../features/preferences/PreferenceProvider'
import { DEFAULT_PREFERENCES } from '../../features/preferences/preferenceDefaults'
import PreferenceControlGroup from './PreferenceControlGroup'

const focusable = 'button:not([disabled]), input:not([disabled]), select:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])'
const statusKeys = { idle: 'statusIdle', loading: 'statusLoading', saving: 'statusSaving', synced: 'statusSynced', unsynced: 'statusUnsynced', conflict: 'statusConflict' }

export default function SettingsDrawer({ open, onClose, openerRef }) {
  const overlayRef = useRef(null)
  const dialogRef = useRef(null)
  const closeRef = useRef(null)
  const resetTriggerRef = useRef(null)
  const cancelResetRef = useRef(null)
  const wasConfirmingRef = useRef(false)
  const [confirmReset, setConfirmReset] = useState(false)
  const { preferences, updatePreference, status, retry, translate } = usePreferences()
  const closeFromKeyboard = useEffectEvent(() => { setConfirmReset(false); onClose() })

  useLayoutEffect(() => {
    if (!open) return undefined
    const previousOverflow = document.body.style.overflow
    const opener = openerRef?.current
    const background = Array.from(document.body.children)
      .filter((element) => element !== overlayRef.current)
      .map((element) => ({
        element,
        wasInert: element.hasAttribute('inert'),
        ariaHidden: element.getAttribute('aria-hidden'),
      }))
    for (const { element } of background) {
      element.setAttribute('inert', '')
      element.setAttribute('aria-hidden', 'true')
    }
    document.body.style.overflow = 'hidden'
    closeRef.current?.focus()
    const onKeyDown = (event) => {
      if (event.key === 'Escape') { event.preventDefault(); closeFromKeyboard(); return }
      if (event.key !== 'Tab') return
      const elements = Array.from(dialogRef.current?.querySelectorAll(focusable) ?? [])
      if (!elements.length) { event.preventDefault(); dialogRef.current?.focus(); return }
      const first = elements[0]
      const last = elements[elements.length - 1]
      if (event.shiftKey && (document.activeElement === first || !dialogRef.current?.contains(document.activeElement))) { event.preventDefault(); last.focus() }
      else if (!event.shiftKey && (document.activeElement === last || !dialogRef.current?.contains(document.activeElement))) { event.preventDefault(); first.focus() }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = previousOverflow
      for (const { element, wasInert, ariaHidden } of background) {
        if (!wasInert) element.removeAttribute('inert')
        if (ariaHidden === null) element.removeAttribute('aria-hidden')
        else element.setAttribute('aria-hidden', ariaHidden)
      }
      opener?.focus()
    }
  }, [open, openerRef])

  useLayoutEffect(() => {
    if (open && confirmReset) cancelResetRef.current?.focus()
    else if (open && wasConfirmingRef.current) resetTriggerRef.current?.focus()
    wasConfirmingRef.current = confirmReset
  }, [open, confirmReset])

  if (!open) return null
  const close = () => { setConfirmReset(false); onClose() }
  const reset = () => {
    for (const [key, value] of Object.entries(DEFAULT_PREFERENCES)) updatePreference(key, value)
    setConfirmReset(false)
  }
  return createPortal(
    <div ref={overlayRef} className="settings-overlay">
      <button type="button" className="settings-backdrop" aria-label={translate('closeByBackdrop', 'Đóng cài đặt bằng nền')} tabIndex={-1} data-testid="settings-backdrop" onClick={close} />
      <section ref={dialogRef} className="settings-drawer" role="dialog" aria-modal="true" aria-labelledby="settings-title" tabIndex={-1}>
        <div className="settings-heading">
          <h2 id="settings-title" className="font-display">{translate('settings', 'Cài đặt')}</h2>
          <button ref={closeRef} type="button" className="settings-close" aria-label={translate('closeSettings', 'Đóng cài đặt')} onClick={close}><X aria-hidden="true" /></button>
        </div>
        <div className="settings-scroll">
          <p className="settings-intro">{translate('settingsIntro', 'Tùy chỉnh trải nghiệm học của bạn.')}</p>
          <p className="settings-status" role="status">{translate(statusKeys[status] ?? statusKeys.idle, statusKeys[status] ?? statusKeys.idle)}</p>
          {status === 'unsynced' && <Button variant="ghost" size="sm" onClick={retry}>Thử lại</Button>}
          <PreferenceControlGroup preferences={preferences} updatePreference={updatePreference} />
          {confirmReset ? (
            <div className="settings-confirm" role="group" aria-label="Xác nhận khôi phục">
              <p>{translate('confirmReset', 'Vui lòng xác nhận khôi phục tất cả thiết lập mặc định.')}</p>
              <div className="settings-confirm-actions">
                <Button ref={cancelResetRef} variant="secondary" size="sm" onClick={() => setConfirmReset(false)}>{translate('cancel', 'Hủy')}</Button>
                <Button variant="primary" size="sm" onClick={reset}>{translate('confirmReset', 'Xác nhận khôi phục')}</Button>
              </div>
            </div>
          ) : (
            <fieldset className="settings-group settings-reset-group">
              <legend>{translate('resetLegend', 'Đặt lại')}</legend>
              <Button ref={resetTriggerRef} variant="ghost" size="sm" onClick={() => setConfirmReset(true)}>{translate('reset', 'Khôi phục mặc định')}</Button>
            </fieldset>
          )}
        </div>
      </section>
    </div>, document.body,
  )
}
