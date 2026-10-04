import { Check, Key, LogOut, User } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import { useAuth } from '../features/auth/AuthProvider'
import { usePreferences } from '../features/preferences/PreferenceProvider'
import PreferenceControlGroup from '../components/settings/PreferenceControlGroup'
import { changePassword, getProfile, updateProfile } from '../services/profileApi'

const MIN_DAILY_MINUTES = 5
const MAX_DAILY_MINUTES = 240
const MIN_STUDY_DAYS = 1
const MAX_STUDY_DAYS = 7

function localIsoDate() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

function profileValues(data) {
  return {
    firstName: data?.firstName || '',
    targetBand: data?.targetBand != null ? String(data.targetBand) : '',
    targetExamDate: data?.targetExamDate || '',
    perceivedWeakestSkill: data?.perceivedWeakestSkill || '',
    dailyStudyMinutes: data?.dailyStudyMinutes != null ? String(data.dailyStudyMinutes) : '30',
    studyDaysPerWeek: data?.studyDaysPerWeek != null ? String(data.studyDaysPerWeek) : '5',
  }
}

function validateProfile(values) {
  const errors = {}
  if (values.targetBand !== '') {
    const band = Number(values.targetBand)
    if (!Number.isFinite(band) || band < 0 || band > 9 || Math.abs(band * 2 - Math.round(band * 2)) > 1e-9) {
      errors.targetBand = 'Mục tiêu Band phải nằm trong khoảng 0.0–9.0 và tăng theo 0.5.'
    }
  }
  if (values.targetExamDate && values.targetExamDate < localIsoDate()) {
    errors.targetExamDate = 'Ngày thi dự kiến phải là hôm nay hoặc trong tương lai.'
  }
  if (values.dailyStudyMinutes !== '') {
    const minutes = Number(values.dailyStudyMinutes)
    if (!Number.isInteger(minutes) || minutes < MIN_DAILY_MINUTES || minutes > MAX_DAILY_MINUTES) {
      errors.dailyStudyMinutes = `Thời gian học phải từ ${MIN_DAILY_MINUTES} đến ${MAX_DAILY_MINUTES} phút.`
    }
  }
  if (values.studyDaysPerWeek !== '') {
    const days = Number(values.studyDaysPerWeek)
    if (!Number.isInteger(days) || days < MIN_STUDY_DAYS || days > MAX_STUDY_DAYS) {
      errors.studyDaysPerWeek = `Số ngày học phải từ ${MIN_STUDY_DAYS} đến ${MAX_STUDY_DAYS}.`
    }
  }
  return errors
}

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const { preferences, updatePreference } = usePreferences()

  const [profile, setProfile] = useState(null)
  const [initialValues, setInitialValues] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState({ type: '', text: '' })

  // Form states
  const [firstName, setFirstName] = useState('')
  const [targetBand, setTargetBand] = useState('')
  const [targetExamDate, setTargetExamDate] = useState('')
  const [perceivedWeakestSkill, setPerceivedWeakestSkill] = useState('')
  const [dailyStudyMinutes, setDailyStudyMinutes] = useState('')
  const [studyDaysPerWeek, setStudyDaysPerWeek] = useState('')
  const [selfReportedLevel, setSelfReportedLevel] = useState('')

  // Password change states
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [passwordSaving, setPasswordSaving] = useState(false)
  const [passwordMessage, setPasswordMessage] = useState({ type: '', text: '' })

  useEffect(() => {
    let active = true
    setLoading(true)
    getProfile()
      .then((data) => {
        if (!active) return
        setProfile(data)
        const values = profileValues(data)
        setInitialValues(values)
        setFirstName(values.firstName)
        setTargetBand(values.targetBand)
        setTargetExamDate(values.targetExamDate)
        setPerceivedWeakestSkill(values.perceivedWeakestSkill)
        setDailyStudyMinutes(values.dailyStudyMinutes)
        setStudyDaysPerWeek(values.studyDaysPerWeek)
        setSelfReportedLevel(data.selfReportedLevel || 'INTERMEDIATE')
      })
      .catch((err) => {
        if (!active) return
        setMessage({ type: 'error', text: err.message || 'Không thể tải thông tin cá nhân.' })
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [])

  async function handleSaveProfile(event) {
    event.preventDefault()
    setMessage({ type: '', text: '' })
    const values = { firstName, targetBand, targetExamDate, perceivedWeakestSkill, dailyStudyMinutes, studyDaysPerWeek }
    const errors = validateProfile(values)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return
    setSaving(true)

    try {
      const updated = await updateProfile({
        firstName: firstName.trim(),
        targetBand: targetBand ? Number(targetBand) : null,
        targetExamDate: targetExamDate || null,
        perceivedWeakestSkill: perceivedWeakestSkill || null,
        dailyStudyMinutes: dailyStudyMinutes ? Number(dailyStudyMinutes) : null,
        studyDaysPerWeek: studyDaysPerWeek ? Number(studyDaysPerWeek) : null,
        selfReportedLevel: selfReportedLevel || null,
        onboardingVersion: profile?.onboardingVersion,
      })
      setProfile(updated)
      const nextValues = profileValues(updated)
      setInitialValues(nextValues)
      setFirstName(nextValues.firstName)
      setTargetBand(nextValues.targetBand)
      setTargetExamDate(nextValues.targetExamDate)
      setPerceivedWeakestSkill(nextValues.perceivedWeakestSkill)
      setDailyStudyMinutes(nextValues.dailyStudyMinutes)
      setStudyDaysPerWeek(nextValues.studyDaysPerWeek)
      setMessage({ type: 'success', text: 'Đã lưu thay đổi.' })
    } catch (err) {
      setMessage({ type: 'error', text: err.message || 'Không thể lưu thay đổi lúc này. Vui lòng thử lại.' })
    } finally {
      setSaving(false)
    }
  }

  async function handleChangePassword(event) {
    event.preventDefault()
    setPasswordMessage({ type: '', text: '' })

    if (newPassword !== confirmPassword) {
      setPasswordMessage({ type: 'error', text: 'Mật khẩu mới và xác nhận mật khẩu không khớp.' })
      return
    }

    if (newPassword.length < 8) {
      setPasswordMessage({ type: 'error', text: 'Mật khẩu mới phải có ít nhất 8 ký tự.' })
      return
    }

    setPasswordSaving(true)
    try {
      await changePassword(currentPassword, newPassword)
      setPasswordMessage({ type: 'success', text: 'Đổi mật khẩu thành công!' })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (err) {
      setPasswordMessage({ type: 'error', text: err.message || 'Đổi mật khẩu thất bại.' })
    } finally {
      setPasswordSaving(false)
    }
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const currentValues = { firstName, targetBand, targetExamDate, perceivedWeakestSkill, dailyStudyMinutes, studyDaysPerWeek }
  const isDirty = initialValues ? JSON.stringify(currentValues) !== JSON.stringify(initialValues) : false

  return (
    <section className="profile-page" aria-labelledby="profile-title">
      <div className="search-page-header" style={{ marginBottom: '2rem' }}>
        <p className="eyebrow">HỒ SƠ HỌC VIÊN</p>
        <h1 id="profile-title" className="font-display" style={{ fontSize: '2.25rem', marginBottom: '0.5rem' }}>
          Hồ sơ &amp; Cài đặt
        </h1>
        <p className="foundation-copy">
          Quản lý thông tin cá nhân, mục tiêu IELTS và tùy biến trải nghiệm luyện tập.
        </p>
      </div>

      {loading && <p className="search-status" role="status">Đang tải hồ sơ…</p>}

      {!loading && (
        <div style={{ display: 'grid', gap: '2rem' }}>
          {message.text && (
            <div
              role="alert"
              style={{
                padding: '0.875rem 1.25rem',
                borderRadius: '8px',
                background: message.type === 'success' ? 'rgba(34, 197, 94, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                border: message.type === 'success' ? '1px solid rgba(34, 197, 94, 0.4)' : '1px solid rgba(239, 68, 68, 0.4)',
                color: message.type === 'success' ? '#4ade80' : '#f87171',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
              }}
            >
              {message.type === 'success' && <Check size={18} />}
              <span>{message.text}</span>
            </div>
          )}

          {/* Section 1: Personal Info & Goals */}
          <GlassCard className="profile-section-card">
            <h2 style={{ fontSize: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.5rem' }}>
              <User size={20} style={{ color: '#d97706' }} /> Hồ sơ &amp; Mục tiêu
            </h2>

            <form onSubmit={handleSaveProfile} style={{ display: 'grid', gap: '1.25rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
                <div>
                  <label htmlFor="profile-name" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                    Họ và tên hiển thị
                  </label>
                  <input
                    id="profile-name"
                    type="text"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    required
                    style={{
                      width: '100%',
                      padding: '0.65rem 0.875rem',
                      background: 'rgba(255, 255, 255, 0.05)',
                      border: '1px solid rgba(255, 255, 255, 0.15)',
                      borderRadius: '6px',
                      color: '#fff',
                    }}
                  />
                </div>

                <div>
                  <label htmlFor="profile-email" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                    Email (Không thể thay đổi)
                  </label>
                  <input
                    id="profile-email"
                    type="email"
                    value={profile?.email || user?.email || ''}
                    disabled
                    style={{
                      width: '100%',
                      padding: '0.65rem 0.875rem',
                      background: 'rgba(255, 255, 255, 0.02)',
                      border: '1px solid rgba(255, 255, 255, 0.08)',
                      borderRadius: '6px',
                      color: 'rgba(255, 255, 255, 0.5)',
                      cursor: 'not-allowed',
                    }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                <div>
                  <label htmlFor="profile-target-band" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                    Mục tiêu Band IELTS (0.0 - 9.0)
                  </label>
                  <input
                    id="profile-target-band"
                    type="number"
                    step="0.5"
                    min="0"
                    max="9"
                    value={targetBand}
                    onChange={(e) => setTargetBand(e.target.value)}
                    placeholder="7.0"
                    aria-invalid={Boolean(fieldErrors.targetBand)}
                    style={{
                      width: '100%',
                      padding: '0.65rem 0.875rem',
                      background: 'rgba(255, 255, 255, 0.05)',
                      border: '1px solid rgba(255, 255, 255, 0.15)',
                      borderRadius: '6px',
                      color: '#fff',
                    }}
                  />
                  {fieldErrors.targetBand && <p className="profile-field-error" role="alert">{fieldErrors.targetBand}</p>}
                </div>

                <div>
                  <label htmlFor="profile-exam-date" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                    Ngày thi dự kiến
                  </label>
                  <input
                    id="profile-exam-date"
                    type="date"
                    value={targetExamDate}
                    onChange={(e) => {
                      const value = e.target.value
                      setTargetExamDate(value)
                      setFieldErrors((current) => ({
                        ...current,
                        targetExamDate: value && value < localIsoDate()
                          ? 'Ngày thi dự kiến phải là hôm nay hoặc trong tương lai.'
                          : undefined,
                      }))
                    }}
                    min={localIsoDate()}
                    aria-invalid={Boolean(fieldErrors.targetExamDate)}
                    style={{
                      width: '100%',
                      padding: '0.65rem 0.875rem',
                      background: 'rgba(255, 255, 255, 0.05)',
                      border: '1px solid rgba(255, 255, 255, 0.15)',
                      borderRadius: '6px',
                      color: '#fff',
                    }}
                  />
                  {fieldErrors.targetExamDate && <p className="profile-field-error" role="alert">{fieldErrors.targetExamDate}</p>}
                </div>

                <div>
                  <label htmlFor="profile-weak-skill" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                    Kỹ năng muốn ưu tiên
                  </label>
                  <select
                    id="profile-weak-skill"
                    value={perceivedWeakestSkill}
                    onChange={(e) => setPerceivedWeakestSkill(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '0.65rem 0.875rem',
                      background: '#18181b',
                      border: '1px solid rgba(255, 255, 255, 0.15)',
                      borderRadius: '6px',
                      color: '#fff',
                    }}
                  >
                    <option value="">Chưa chọn</option>
                    <option value="READING">Reading</option>
                    <option value="LISTENING">Listening</option>
                    <option value="WRITING">Writing</option>
                    <option value="SPEAKING">Speaking</option>
                  </select>
                  <p className="profile-helper-text">Đây là lựa chọn của bạn; hệ thống vẫn phân tích điểm yếu dựa trên kết quả luyện tập.</p>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                <div>
                  <label htmlFor="profile-daily-minutes" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                    Thời gian học mỗi ngày (phút)
                  </label>
                  <input
                    id="profile-daily-minutes"
                    type="number"
                    min="5"
                    max="240"
                    value={dailyStudyMinutes}
                    onChange={(e) => setDailyStudyMinutes(e.target.value)}
                    aria-invalid={Boolean(fieldErrors.dailyStudyMinutes)}
                    style={{
                      width: '100%',
                      padding: '0.65rem 0.875rem',
                      background: 'rgba(255, 255, 255, 0.05)',
                      border: '1px solid rgba(255, 255, 255, 0.15)',
                      borderRadius: '6px',
                      color: '#fff',
                    }}
                  />
                  {fieldErrors.dailyStudyMinutes && <p className="profile-field-error" role="alert">{fieldErrors.dailyStudyMinutes}</p>}
                </div>

                <div>
                  <label htmlFor="profile-days-week" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                    Số ngày học / tuần
                  </label>
                  <input
                    id="profile-days-week"
                    type="number"
                    min="1"
                    max="7"
                    value={studyDaysPerWeek}
                    onChange={(e) => setStudyDaysPerWeek(e.target.value)}
                    aria-invalid={Boolean(fieldErrors.studyDaysPerWeek)}
                    style={{
                      width: '100%',
                      padding: '0.65rem 0.875rem',
                      background: 'rgba(255, 255, 255, 0.05)',
                      border: '1px solid rgba(255, 255, 255, 0.15)',
                      borderRadius: '6px',
                      color: '#fff',
                    }}
                  />
                  {fieldErrors.studyDaysPerWeek && <p className="profile-field-error" role="alert">{fieldErrors.studyDaysPerWeek}</p>}
                </div>
              </div>

              <div style={{ marginTop: '0.5rem' }}>
                <Button variant="primary" size="md" type="submit" disabled={!isDirty || saving}>
                  {saving ? 'Đang lưu…' : 'Lưu thay đổi hồ sơ'}
                </Button>
              </div>
            </form>
          </GlassCard>

          <GlassCard className="profile-preferences-card">
            <h2 className="profile-card-heading">Tùy chọn học tập</h2>
            <PreferenceControlGroup preferences={preferences} updatePreference={updatePreference} />
          </GlassCard>

          {/* Section 3: Security */}
          <GlassCard className="profile-password-card">
            <h2 style={{ fontSize: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.5rem' }}>
              <Key size={20} style={{ color: '#d97706' }} /> Bảo mật
            </h2>
            <p className="profile-helper-text">Đổi mật khẩu và bảo vệ tài khoản của bạn.</p>

            {passwordMessage.text && (
              <div
                role="alert"
                style={{
                  padding: '0.875rem 1.25rem',
                  borderRadius: '8px',
                  marginBottom: '1rem',
                  background: passwordMessage.type === 'success' ? 'rgba(34, 197, 94, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                  border: passwordMessage.type === 'success' ? '1px solid rgba(34, 197, 94, 0.4)' : '1px solid rgba(239, 68, 68, 0.4)',
                  color: passwordMessage.type === 'success' ? '#4ade80' : '#f87171',
                }}
              >
                {passwordMessage.text}
              </div>
            )}

            <form onSubmit={handleChangePassword} style={{ display: 'grid', gap: '1rem', maxWidth: '480px' }}>
              <div>
                <label htmlFor="current-pass" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                  Mật khẩu hiện tại
                </label>
                <input
                  id="current-pass"
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.875rem',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.15)',
                    borderRadius: '6px',
                    color: '#fff',
                  }}
                />
              </div>

              <div>
                <label htmlFor="new-pass" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                  Mật khẩu mới (Tối thiểu 8 ký tự)
                </label>
                <input
                  id="new-pass"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  minLength={8}
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.875rem',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.15)',
                    borderRadius: '6px',
                    color: '#fff',
                  }}
                />
              </div>

              <div>
                <label htmlFor="confirm-pass" style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.35rem', color: 'rgba(255, 255, 255, 0.8)' }}>
                  Xác nhận mật khẩu mới
                </label>
                <input
                  id="confirm-pass"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  minLength={8}
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.875rem',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.15)',
                    borderRadius: '6px',
                    color: '#fff',
                  }}
                />
              </div>

              <div style={{ marginTop: '0.5rem' }}>
                <Button variant="secondary" size="md" type="submit" disabled={passwordSaving}>
                  {passwordSaving ? 'Đang cập nhật…' : 'Cập nhật mật khẩu'}
                </Button>
              </div>
            </form>
          </GlassCard>

          {/* Section 3: Account & Session */}
          <GlassCard className="profile-session-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h3 style={{ fontSize: '1rem', marginBottom: '0.25rem' }}>Đăng xuất khỏi thiết bị này</h3>
              <p style={{ fontSize: '0.875rem', color: 'rgba(255, 255, 255, 0.6)' }}>
                Phiên đăng nhập hiện tại sẽ được thu hồi an toàn.
              </p>
            </div>
            <Button variant="ghost" size="sm" onClick={handleLogout} style={{ color: '#f87171' }}>
              <LogOut size={16} style={{ marginRight: '0.5rem' }} /> Đăng xuất
            </Button>
          </GlassCard>
        </div>
      )}
    </section>
  )
}
