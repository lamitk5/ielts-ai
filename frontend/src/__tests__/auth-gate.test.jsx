import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import AuthGate from '../components/auth/AuthGate'
import FloatingTutor from '../components/tutor/FloatingTutor'
import { AuthProvider } from '../features/auth/AuthProvider'
import { sendTutorMessage } from '../services/aiTutorApi'

function Destination() {
  const location = useLocation()
  return <output>{JSON.stringify({ path: location.pathname, from: location.state?.from })}</output>
}

function renderGate(returnTo) {
  return render(
    <MemoryRouter initialEntries={['/practice/writing?task=1#prompt']}>
      <AuthProvider>
        <Routes>
          <Route path="/practice/writing" element={<AuthGate reason="Lưu hỗ trợ cho bài Writing"><p>Private tutor composer</p></AuthGate>} />
          <Route path="/login" element={<Destination />} />
          <Route path="/register" element={<Destination />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  localStorage.clear()
  vi.stubGlobal('fetch', vi.fn())
})

describe('personalized Tutor auth gate', () => {
  test('guest opening shows guidance and no composer or private context request', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter initialEntries={['/practice/writing']}><AuthProvider><FloatingTutor context={{ skill: 'WRITING', exerciseId: 'private-1' }} /></AuthProvider></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    expect(screen.getByRole('dialog', { name: 'Trợ giảng AI' })).toBeInTheDocument()
    expect(screen.getByText(/đăng nhập/i)).toBeInTheDocument()
    expect(screen.queryByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Gửi câu hỏi' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/đính kèm/i)).not.toBeInTheDocument()
    expect(global.fetch).not.toHaveBeenCalled()
  })

  test.each([['Đăng nhập', '/login'], ['Tạo tài khoản', '/register']])('%s preserves the current internal route', async (label, path) => {
    const user = userEvent.setup()
    renderGate()
    await user.click(screen.getByRole('link', { name: label }))
    expect(screen.getByText(JSON.stringify({ path, from: '/practice/writing?task=1#prompt' }))).toBeInTheDocument()
  })

  test('an external returnTo is discarded', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/practice/writing']}>
        <AuthProvider><Routes>
          <Route path="/practice/writing" element={<AuthGate returnTo="https://evil.example/steal"><p>Private</p></AuthGate>} />
          <Route path="/login" element={<Destination />} />
        </Routes></AuthProvider>
      </MemoryRouter>,
    )
    await user.click(screen.getByRole('link', { name: 'Đăng nhập' }))
    expect(screen.getByText(JSON.stringify({ path: '/login', from: '/practice/writing' }))).toBeInTheDocument()
  })

  test('authenticated learner sees the protected composer', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'member-token', user: { id: 'member-1' } }))
    const user = userEvent.setup()
    render(<MemoryRouter initialEntries={['/practice/writing']}><AuthProvider><FloatingTutor /></AuthProvider></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Đăng nhập' })).not.toBeInTheDocument()
  })

  test('401 is normalized to AUTH_REQUIRED without server details', async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 401, json: async () => ({ error: { code: 'AI_PROVIDER_ERROR', message: 'secret provider trace' } }) })
    await expect(sendTutorMessage({ message: 'Help', context: { skill: 'GENERAL' } })).rejects.toMatchObject({ code: 'AUTH_REQUIRED', status: 401, message: expect.not.stringContaining('secret provider trace') })
  })

  test('a session rejected by the server returns to the auth guidance', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'expired', user: { id: 'member-1' } }))
    global.fetch.mockResolvedValue({ ok: false, status: 401, json: async () => ({ error: { code: 'AUTH_REQUIRED', message: 'private backend detail' } }) })
    const user = userEvent.setup()
    render(<MemoryRouter initialEntries={['/practice/writing']}><AuthProvider><FloatingTutor /></AuthProvider></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: 'Mở Trợ giảng AI' }))
    await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Trợ giảng AI' }), 'Question')
    await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }))
    await waitFor(() => expect(screen.getByRole('link', { name: 'Đăng nhập' })).toBeInTheDocument())
    expect(screen.queryByText('private backend detail')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Thử lại' })).not.toBeInTheDocument()
  })
})
