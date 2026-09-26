import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, afterEach, describe, expect, test, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../features/auth/AuthProvider'
import Navbar from '../components/layout/Navbar'
import { DEFAULT_PREFERENCES } from '../features/preferences/preferenceDefaults'

const account = { id: 'learner-1', email: 'learner@example.com' }
const server = { ...DEFAULT_PREFERENCES, version: 1 }

function renderNavbar() {
  return render(<MemoryRouter><AuthProvider><Navbar /></AuthProvider></MemoryRouter>)
}

beforeEach(() => {
  localStorage.clear()
  localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({ token: 'token', user: account }))
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => server }))
})
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); localStorage.clear() })

describe('Settings drawer', () => {
  test('opens from authenticated navigation with an accessible name and returns focus after Escape', async () => {
    const user = userEvent.setup()
    renderNavbar()
    const opener = screen.getByRole('button', { name: 'Cài đặt' })
    await user.click(opener)
    const dialog = screen.getByRole('dialog', { name: 'Cài đặt' })
    expect(dialog).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Đóng cài đặt' })).toHaveFocus()
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })

  test('makes the background inert to assistive technology and restores its prior state', async () => {
    const user = userEvent.setup()
    const { container } = renderNavbar()
    const sibling = document.createElement('aside')
    sibling.setAttribute('aria-hidden', 'false')
    sibling.setAttribute('inert', '')
    document.body.appendChild(sibling)
    container.setAttribute('aria-hidden', 'false')
    try {
      await user.click(screen.getByRole('button', { name: 'Cài đặt' }))
      expect(container).toHaveAttribute('inert')
      expect(container).toHaveAttribute('aria-hidden', 'true')
      expect(sibling).toHaveAttribute('aria-hidden', 'true')
      expect(screen.getByRole('dialog', { name: 'Cài đặt' })).not.toHaveAttribute('inert')
      await user.keyboard('{Escape}')
      expect(container).not.toHaveAttribute('inert')
      expect(container).toHaveAttribute('aria-hidden', 'false')
      expect(sibling).toHaveAttribute('inert')
      expect(sibling).toHaveAttribute('aria-hidden', 'false')
    } finally { sibling.remove() }
  })

  test('traps Tab in the drawer and closes on the backdrop', async () => {
    const user = userEvent.setup()
    renderNavbar()
    const opener = screen.getByRole('button', { name: 'Cài đặt' })
    await user.click(opener)
    const dialog = screen.getByRole('dialog', { name: 'Cài đặt' })
    const close = within(dialog).getByRole('button', { name: 'Đóng cài đặt' })
    close.focus()
    await user.keyboard('{Shift>}{Tab}{/Shift}')
    expect(within(dialog).getByRole('button', { name: 'Khôi phục mặc định' })).toHaveFocus()
    await user.click(screen.getByTestId('settings-backdrop'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })

  test('keyboard selection previews immediately and reset needs confirmation', async () => {
    const user = userEvent.setup()
    renderNavbar()
    await user.click(screen.getByRole('button', { name: 'Cài đặt' }))
    await screen.findByText('Đã đồng bộ')
    const dialog = screen.getByRole('dialog', { name: 'Cài đặt' })
    const accent = within(dialog).getByRole('combobox', { name: 'Màu nhấn' })
    await user.selectOptions(accent, 'emerald')
    expect(within(dialog).getByText('Đang lưu')).toBeInTheDocument()
    expect(accent).toHaveValue('emerald')
    expect(document.documentElement.style.getPropertyValue('--accent')).toBe('#216c56')
    expect(within(dialog).getByText(/Màu nhấn: Emerald/)).toBeInTheDocument()
    await user.selectOptions(within(dialog).getByRole('combobox', { name: 'Tỷ lệ khung Reading' }), '60')
    await user.click(within(dialog).getByRole('checkbox', { name: 'Gợi ý từ Trợ giảng AI' }))
    await user.click(within(dialog).getByRole('button', { name: 'Khôi phục mặc định' }))
    expect(within(dialog).getByRole('group', { name: 'Xác nhận khôi phục' })).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Hủy' })).toHaveFocus()
    expect(accent).toHaveValue('emerald')
    await user.click(within(dialog).getByRole('button', { name: 'Hủy' }))
    expect(within(dialog).getByRole('button', { name: 'Khôi phục mặc định' })).toHaveFocus()
    expect(accent).toHaveValue('emerald')
    await user.click(within(dialog).getByRole('button', { name: 'Khôi phục mặc định' }))
    await user.click(within(dialog).getByRole('button', { name: 'Xác nhận khôi phục' }))
    expect(accent).toHaveValue('gold')
    expect(within(dialog).getByRole('combobox', { name: 'Tỷ lệ khung Reading' })).toHaveValue('40')
    expect(within(dialog).getByRole('checkbox', { name: 'Gợi ý từ Trợ giảng AI' })).not.toBeChecked()
  })

  test('live preview includes theme and motion selections', async () => {
    const user = userEvent.setup()
    renderNavbar()
    await user.click(screen.getByRole('button', { name: 'Cài đặt' }))
    const dialog = screen.getByRole('dialog', { name: 'Cài đặt' })
    await user.selectOptions(within(dialog).getByRole('combobox', { name: 'Giao diện' }), 'dark')
    await user.selectOptions(within(dialog).getByRole('combobox', { name: 'Chuyển động' }), 'reduce')
    expect(within(dialog).getByText(/Giao diện: Tối/)).toBeInTheDocument()
    expect(within(dialog).getByText(/Chuyển động: Giảm chuyển động/)).toBeInTheDocument()
  })

  test('shows unsynced preference status and a retry action after a failed save', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn().mockResolvedValueOnce({ ok: true, json: async () => server })
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValue({ ok: true, json: async () => ({ ...server, accentPreset: 'EMERALD', version: 2 }) })
    vi.stubGlobal('fetch', fetchMock)
    renderNavbar()
    await user.click(screen.getByRole('button', { name: 'Cài đặt' }))
    await screen.findByText('Đã đồng bộ')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Màu nhấn' }), 'emerald')
    await screen.findByText('Chưa đồng bộ')
    await user.click(screen.getByRole('button', { name: 'Thử lại' }))
    await waitFor(() => expect(screen.getByText('Đã đồng bộ')).toBeInTheDocument())
  })

  test('announces a server version conflict after reconciliation', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => server })
      .mockResolvedValueOnce({ ok: false, status: 409, json: async () => ({ error: { code: 'VERSION_CONFLICT' } }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ ...server, accentPreset: 'BURGUNDY', version: 2 }) }))
    renderNavbar()
    await user.click(screen.getByRole('button', { name: 'Cài đặt' }))
    await screen.findByText('Đã đồng bộ')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Màu nhấn' }), 'emerald')
    await screen.findByText('Đã cập nhật từ tài khoản')
    expect(screen.getByRole('combobox', { name: 'Màu nhấn' })).toHaveValue('burgundy')
  })

  test('mobile navigation entry opens the same full-height dialog', async () => {
    const user = userEvent.setup()
    renderNavbar()
    await user.click(screen.getByRole('button', { name: 'Open navigation menu' }))
    await user.click(screen.getByRole('button', { name: 'Cài đặt' }))
    expect(screen.getByRole('dialog', { name: 'Cài đặt' })).toHaveAttribute('aria-modal', 'true')
    expect(screen.getByRole('button', { name: 'Open navigation menu', hidden: true })).toHaveAttribute('aria-expanded', 'false')
  })

  test('logging out clears account appearance from the shell', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn((url) => Promise.resolve(url === '/api/user/preferences'
      ? { ok: true, json: async () => ({ ...server, accentPreset: 'EMERALD' }) }
      : { ok: true, status: 204, json: async () => null })))
    renderNavbar()
    await waitFor(() => expect(document.documentElement.style.getPropertyValue('--accent')).toBe('#216c56'))
    await user.click(screen.getByRole('button', { name: 'Đăng xuất' }))
    await waitFor(() => expect(screen.getByRole('link', { name: 'Đăng nhập' })).toBeInTheDocument())
    expect(document.documentElement.style.getPropertyValue('--accent')).toBe('#8a6426')
  })
})
