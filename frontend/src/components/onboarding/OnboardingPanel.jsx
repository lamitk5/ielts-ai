import { useCallback, useEffect, useState } from 'react'
import { completeOnboarding, getOnboarding, onboardingOptions, saveOnboarding } from '../../services/onboardingApi'

const SKILL_LABELS = {
  READING: 'Reading',
  LISTENING: 'Listening',
  WRITING: 'Writing',
  SPEAKING: 'Speaking',
}

const LEVEL_LABELS = {
  BEGINNER: 'Sơ cấp',
  INTERMEDIATE: 'Trung cấp',
  ADVANCED: 'Nâng cao',
}

const INITIAL = {
  selfReportedLevel: 'INTERMEDIATE',
  targetBand: 6.0,
  targetExamDate: '',
  perceivedWeakestSkill: 'READING',
  dailyStudyMinutes: 45,
  studyDaysPerWeek: 5,
  version: 0,
}

export function OnboardingPanel({ onClose }) {
  const [goal, setGoal] = useState(INITIAL)
  const [profile, setProfile] = useState(null)
  const [status, setStatus] = useState('LOADING')
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    getOnboarding()
      .then((record) => {
        if (!active) return
        setProfile(record)
        setGoal({
          selfReportedLevel: record.selfReportedLevel ?? INITIAL.selfReportedLevel,
          targetBand: record.targetBand ?? INITIAL.targetBand,
          targetExamDate: record.targetExamDate ?? '',
          perceivedWeakestSkill: record.perceivedWeakestSkill ?? INITIAL.perceivedWeakestSkill,
          dailyStudyMinutes: record.dailyStudyMinutes ?? INITIAL.dailyStudyMinutes,
          studyDaysPerWeek: record.studyDaysPerWeek ?? INITIAL.studyDaysPerWeek,
          version: record.version,
        })
        setStatus(record.state === 'NOT_STARTED' ? 'NEW' : 'READY')
      })
      .catch(() => { if (active) setStatus('ERROR') })
    return () => { active = false }
  }, [])

  const update = useCallback((key, value) => setGoal((current) => ({ ...current, [key]: value })), [])

  const save = useCallback(async (state) => {
    setStatus('SAVING')
    try {
      const saved = state === 'IN_PROGRESS'
        ? await saveOnboarding({ ...goal, state })
        : await completeOnboarding(state, goal.version)
      setProfile(saved)
      setGoal((current) => ({ ...current, version: saved.version }))
      setStatus('SAVED')
      setMessage('')
    } catch (error) {
      setStatus(error?.code === 'CONFLICT' ? 'CONFLICT' : 'ERROR')
      setMessage(error?.code === 'CONFLICT'
        ? 'Mục tiêu đã được thay đổi ở nơi khác. Vui lòng tải lại.'
        : 'Không thể lưu mục tiêu lúc này.')
    }
  }, [goal])

  if (status === 'LOADING') return <p role="status">Đang tải thiết lập…</p>
  if (status === 'ERROR') return <p role="alert">Không tải được thiết lập. Bạn vẫn có thể tiếp tục học.</p>
  if (profile?.isSkipped) {
    return (
      <section aria-labelledby="onboarding-heading">
        <h2 id="onboarding-heading">Mục tiêu của bạn</h2>
        <p>Bạn đã bỏ qua thiết lập. Bạn có thể thiết lập sau bất cứ lúc nào.</p>
        {onClose ? <button type="button" onClick={onClose}>Đóng</button> : null}
      </section>
    )
  }

  return (
    <section aria-labelledby="onboarding-heading">
      <h2 id="onboarding-heading">Mục tiêu của bạn</h2>
      <p>Bạn tự nhận định mục tiêu và lịch học. Đây là mục tiêu cá nhân, không phải trình độ đo được.</p>

      <label htmlFor="onboarding-level">Bạn tự nhận định trình độ hiện tại</label>
      <select id="onboarding-level" value={goal.selfReportedLevel}
        onChange={(event) => update('selfReportedLevel', event.target.value)}>
        {onboardingOptions.levels.map((level) => <option key={level} value={level}>{LEVEL_LABELS[level]}</option>)}
      </select>

      <label htmlFor="onboarding-band">Band mục tiêu</label>
      <input id="onboarding-band" type="number" min="0" max="9" step="0.5"
        value={goal.targetBand} onChange={(event) => update('targetBand', Number(event.target.value))} />

      <label htmlFor="onboarding-date">Ngày thi dự kiến (không bắt buộc)</label>
      <input id="onboarding-date" type="date" value={goal.targetExamDate}
        onChange={(event) => update('targetExamDate', event.target.value)} />

      <label htmlFor="onboarding-weak">Kỹ năng bạn cảm thấy yếu nhất</label>
      <select id="onboarding-weak" value={goal.perceivedWeakestSkill}
        onChange={(event) => update('perceivedWeakestSkill', event.target.value)}>
        {onboardingOptions.skills.map((skill) => <option key={skill} value={skill}>{SKILL_LABELS[skill]}</option>)}
      </select>

      <label htmlFor="onboarding-minutes">Số phút học mỗi ngày</label>
      <input id="onboarding-minutes" type="number" min="5" max="240"
        value={goal.dailyStudyMinutes} onChange={(event) => update('dailyStudyMinutes', Number(event.target.value))} />

      <label htmlFor="onboarding-days">Số ngày học mỗi tuần</label>
      <input id="onboarding-days" type="number" min="1" max="7"
        value={goal.studyDaysPerWeek} onChange={(event) => update('studyDaysPerWeek', Number(event.target.value))} />

      {profile?.isSelfReportOnly && profile.state === 'COMPLETED' ? (
        <p>Bạn đã thiết lập mục tiêu ở mức ước tính. Hệ thống chưa đo được trình độ thực tế của bạn.</p>
      ) : null}
      {message ? <p role="alert">{message}</p> : null}

      <button type="button" onClick={() => save('IN_PROGRESS')}>Lưu tạm</button>
      <button type="button" onClick={() => save('COMPLETED')}>Hoàn tất</button>
      <button type="button" onClick={() => save('SKIPPED')}>Bỏ qua, thiết lập sau</button>
    </section>
  )
}
