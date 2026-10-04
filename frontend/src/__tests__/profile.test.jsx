import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthProvider'
import { PreferenceProvider } from '../features/preferences/PreferenceProvider'
import ProfilePage from '../pages/ProfilePage'

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
    sessionStorage.clear()
  })

  test('renders user profile and learning goals', async () => {
    const mockProfile = {
      id: 'user-1',
      email: 'learner@example.com',
      firstName: 'Nguyễn Văn A',
      avatarUrl: null,
      role: 'CUSTOMER',
      targetBand: 7.5,
      targetExamDate: '2026-12-01',
      perceivedWeakestSkill: 'WRITING',
      dailyStudyMinutes: 45,
      studyDaysPerWeek: 5,
      selfReportedLevel: 'INTERMEDIATE',
      onboardingState: 'COMPLETED',
      onboardingVersion: 1,
    }

    global.fetch = vi.fn().mockImplementation((url) => {
      if (url.includes('/api/me/profile')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockProfile),
        })
      }
      if (url.includes('/api/user/preferences')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            themeMode: 'SYSTEM',
            accentPreset: 'GOLD',
            fontScale: 'DEFAULT',
            density: 'DEFAULT',
            reduceMotion: 'SYSTEM',
            version: 0,
          }),
        })
      }
      return Promise.reject(new Error('not found'))
    })

    render(
      <MemoryRouter>
        <AuthProvider>
          <PreferenceProvider>
            <ProfilePage />
          </PreferenceProvider>
        </AuthProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByDisplayValue('Nguyễn Văn A')).toBeInTheDocument()
    expect(screen.getByDisplayValue('learner@example.com')).toBeInTheDocument()
    expect(screen.getByDisplayValue('7.5')).toBeInTheDocument()
    expect(screen.getByDisplayValue('45')).toBeInTheDocument()
    expect(screen.getByText('Bảo mật')).toBeInTheDocument()
  })

  test('validates matching passwords before submit', async () => {
    const mockProfile = {
      id: 'user-1',
      email: 'learner@example.com',
      firstName: 'Nguyễn Văn A',
      role: 'CUSTOMER',
      onboardingVersion: 1,
    }

    global.fetch = vi.fn().mockImplementation((url) => {
      if (url.includes('/api/me/profile')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockProfile),
        })
      }
      if (url.includes('/api/user/preferences')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            themeMode: 'SYSTEM',
            accentPreset: 'GOLD',
            fontScale: 'DEFAULT',
            density: 'DEFAULT',
            reduceMotion: 'SYSTEM',
            version: 0,
          }),
        })
      }
      return Promise.reject(new Error('not found'))
    })

    render(
      <MemoryRouter>
        <AuthProvider>
          <PreferenceProvider>
            <ProfilePage />
          </PreferenceProvider>
        </AuthProvider>
      </MemoryRouter>,
    )

    await screen.findByDisplayValue('Nguyễn Văn A')

    fireEvent.change(screen.getByLabelText('Mật khẩu hiện tại'), { target: { value: 'oldpass123' } })
    fireEvent.change(screen.getByLabelText(/Mật khẩu mới \(Tối thiểu/i), { target: { value: 'newpassword123' } })
    fireEvent.change(screen.getByLabelText('Xác nhận mật khẩu mới'), { target: { value: 'differentpassword' } })

    fireEvent.click(screen.getByRole('button', { name: 'Cập nhật mật khẩu' }))

    expect(await screen.findByText('Mật khẩu mới và xác nhận mật khẩu không khớp.')).toBeInTheDocument()
  })

  test('saves a dirty profile with the authenticated session token', async () => {
    const mockProfile = {
      id: 'user-1', email: 'learner@example.com', firstName: 'Nguyễn Văn A', role: 'CUSTOMER',
      targetBand: 7, targetExamDate: '2026-12-01', perceivedWeakestSkill: 'WRITING',
      dailyStudyMinutes: 45, studyDaysPerWeek: 5, selfReportedLevel: 'INTERMEDIATE', onboardingVersion: 1,
    }
    const savedProfile = { ...mockProfile, firstName: 'Nguyễn Văn B' }
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'session-token', user: { id: 'user-1', email: mockProfile.email } }))
    const fetchMock = vi.fn().mockImplementation((url, options) => {
      if (url.includes('/api/me/profile') && options?.method === 'PUT') {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(savedProfile) })
      }
      if (url.includes('/api/me/profile')) return Promise.resolve({ ok: true, json: () => Promise.resolve(mockProfile) })
      if (url.includes('/api/user/preferences')) return Promise.resolve({ ok: true, json: () => Promise.resolve({ themeMode: 'SYSTEM', accentPreset: 'GOLD', fontScale: 'DEFAULT', density: 'DEFAULT', reduceMotion: 'SYSTEM', version: 0 }) })
      return Promise.reject(new Error('not found'))
    })
    global.fetch = fetchMock

    render(<MemoryRouter><AuthProvider><PreferenceProvider><ProfilePage /></PreferenceProvider></AuthProvider></MemoryRouter>)

    const saveButton = await screen.findByRole('button', { name: 'Lưu thay đổi hồ sơ' })
    expect(saveButton).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Họ và tên hiển thị'), { target: { value: 'Nguyễn Văn B' } })
    expect(saveButton).toBeEnabled()
    fireEvent.click(saveButton)

    expect(await screen.findByText('Đã lưu thay đổi.')).toBeInTheDocument()
    const putCall = fetchMock.mock.calls.find(([, options]) => options?.method === 'PUT')
    expect(putCall[1].headers.Authorization).toBe('Bearer session-token')
  })

  test('prevents past exam dates with an inline validation message', async () => {
    const mockProfile = { id: 'user-1', email: 'learner@example.com', firstName: 'Nguyễn Văn A', role: 'CUSTOMER', onboardingVersion: 1 }
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'session-token', user: { id: 'user-1', email: mockProfile.email } }))
    const fetchMock = vi.fn().mockImplementation((url) => {
      if (url.includes('/api/me/profile')) return Promise.resolve({ ok: true, json: () => Promise.resolve(mockProfile) })
      if (url.includes('/api/user/preferences')) return Promise.resolve({ ok: true, json: () => Promise.resolve({ themeMode: 'SYSTEM', accentPreset: 'GOLD', fontScale: 'DEFAULT', density: 'DEFAULT', reduceMotion: 'SYSTEM', version: 0 }) })
      return Promise.reject(new Error('not found'))
    })
    global.fetch = fetchMock

    render(<MemoryRouter><AuthProvider><PreferenceProvider><ProfilePage /></PreferenceProvider></AuthProvider></MemoryRouter>)

    await screen.findByDisplayValue('Nguyễn Văn A')
    fireEvent.change(screen.getByLabelText('Ngày thi dự kiến'), { target: { value: '2005-07-22' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi hồ sơ' }))

    expect(await screen.findByText('Ngày thi dự kiến phải là hôm nay hoặc trong tương lai.')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([, options]) => options?.method === 'PUT')).toBe(false)
  })
})
