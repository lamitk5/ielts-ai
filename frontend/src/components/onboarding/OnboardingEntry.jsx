import { useCallback, useEffect, useState } from 'react'
import { getOnboarding } from '../../services/onboardingApi'
import { useOptionalAuth } from '../../features/auth/AuthProvider'
import { OnboardingPanel } from './OnboardingPanel'

/**
 * Neutral, optional entry point for a signed-in learner whose goals were never
 * set. It never blocks the journey and never presents a guessed level: it only
 * offers the setup form and keeps the existing adaptive default otherwise.
 */
export function OnboardingEntry() {
  const auth = useOptionalAuth()
  const isAuthenticated = Boolean(auth?.isAuthenticated)
  const [status, setStatus] = useState(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!isAuthenticated) return undefined
    let active = true
    getOnboarding()
      .then((record) => {
        if (!active) return
        const needsSetup = record.state === 'NOT_STARTED' || record.state === 'IN_PROGRESS'
        setStatus(needsSetup ? 'AVAILABLE' : 'DONE')
      })
      .catch(() => { if (active) setStatus('UNAVAILABLE') })
    return () => { active = false }
  }, [isAuthenticated])

  const close = useCallback(() => setOpen(false), [])

  if (!isAuthenticated || open) {
    return open ? <OnboardingPanel onClose={close} /> : null
  }
  if (status !== 'AVAILABLE') return null

  return (
    <section aria-labelledby="onboarding-entry-heading">
      <h2 id="onboarding-entry-heading">Thiết lập mục tiêu học tập</h2>
      <p>Bạn có thể cho biết mục tiêu và lịch học của mình, hoặc bỏ qua và thiết lập sau.</p>
      <button type="button" onClick={() => setOpen(true)}>Thiết lập mục tiêu</button>
    </section>
  )
}