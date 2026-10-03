import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import { createRagAdminApi, readAdminToken, writeAdminToken } from '../services/ragAdminApi'

function renderApp(initialEntry = '/admin/rag', session) {
  if (session) window.localStorage.setItem('ielts-ai-tutor.session', JSON.stringify(session))
  return render(<MemoryRouter initialEntries={[initialEntry]}><App /></MemoryRouter>)
}

beforeEach(() => {
  window.sessionStorage.clear()
  window.localStorage.clear()
  vi.restoreAllMocks()
})

describe('temporary RAG admin CMS', () => {
  test('redirects guests to login without rendering the admin shell', () => {
    renderApp('/admin/rag', null)
    expect(screen.getByRole('heading', { name: 'Đăng nhập' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: /quản trị học liệu IELTS/i })).not.toBeInTheDocument()
  })

  test('uses the authenticated ADMIN session without exposing a token unlock form', async () => {
    window.localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
      token: 'admin-session',
      user: { email: 'admin@example.com', firstName: 'Admin', role: 'ADMIN' },
    }))
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
    vi.stubGlobal('fetch', fetchMock)

    renderApp()

    expect(await screen.findByRole('heading', { name: 'Kho tri thức' })).toBeInTheDocument()
    expect(screen.queryByLabelText(/admin token/i)).not.toBeInTheDocument()
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer admin-session')
  })

  test('persists token in sessionStorage only', () => {
    writeAdminToken('secret')
    expect(readAdminToken()).toBe('secret')
    expect(window.localStorage.getItem('rag-admin-token')).toBeNull()
  })

  test('uploads metadata as multipart', async () => {
    const fetchImpl = vi.fn().mockResolvedValue({ ok: true, json: async () => ({}) })
    const api = createRagAdminApi({ fetchImpl, storage: { getItem: () => 'secret' } })
    const file = new File(['content'], 'guide.txt', { type: 'text/plain' })
    await api.upload({ title: 'Guide', language: 'en', skill: 'GENERAL', rightsNote: 'licensed' }, file)
    const [, options] = fetchImpl.mock.calls[0]
    expect(options.method).toBe('POST')
    expect(options.body).toBeInstanceOf(FormData)
    expect(options.headers['X-Admin-Token']).toBe('secret')
    expect(options.headers.Authorization).toBeUndefined()
    expect(options.headers['Content-Type']).toBeUndefined()
  })

  test('renders pending review actions', async () => {
    writeAdminToken('secret')
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ([{ id: '1', title: 'Guide', skill: 'GENERAL', rightsStatus: 'PENDING_REVIEW', indexStatus: 'NOT_INDEXED', active: false }]) })
      .mockResolvedValueOnce({ ok: true, json: async () => ([]) }))
    renderApp()
    expect(await screen.findByText('Guide')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /duyệt/i }).at(-1)).toBeEnabled()
  })

  test('disables invalid lifecycle actions', async () => {
    writeAdminToken('secret')
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ([{ id: '1', title: 'Guide', skill: 'GENERAL', rightsStatus: 'PENDING_REVIEW', indexStatus: 'NOT_INDEXED', active: false }]) })
      .mockResolvedValueOnce({ ok: true, json: async () => ([]) }))
    renderApp()
    const row = await screen.findByRole('row', { name: /Guide/ })
    expect(within(row).getByRole('button', { name: /kích hoạt/i })).toBeDisabled()
  })

  test('renders bounded preview and job status', async () => {
    writeAdminToken('secret')
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ([{ id: '1', title: 'Guide', skill: 'GENERAL', rightsStatus: 'APPROVED', indexStatus: 'INDEXED', active: true }]) })
      .mockResolvedValueOnce({ ok: true, json: async () => ([{ id: 'job-1', status: 'INDEXED' }]) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ id: '1', title: 'Guide', preview: { text: 'bounded preview', characterCount: 16 } }) }))
    renderApp()
    await userEvent.setup().click(await screen.findByRole('button', { name: /xem/i }))
    expect(await screen.findByText('bounded preview')).toBeInTheDocument()
    expect(screen.getAllByText('Đã lập chỉ mục').length).toBeGreaterThan(0)
  })

  test('handles 401 and 403', async () => {
    const fetchImpl = vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ error: { code: 'RAG_ADMIN_UNAUTHORIZED' } }) })
    const api = createRagAdminApi({ fetchImpl, storage: { getItem: () => 'secret' } })
    await expect(api.list()).rejects.toMatchObject({ status: 401, code: 'RAG_ADMIN_UNAUTHORIZED' })
  })

  test('surfaces upload failures instead of leaving the form pending', async () => {
    writeAdminToken('secret')
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: false, status: 503, json: async () => ({ error: { message: 'RAG service unavailable' } }) })
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    renderApp()
    await screen.findByRole('heading', { name: 'Kho tri thức' })
    await user.type(screen.getByLabelText('Tiêu đề'), 'Guide')
    await user.type(screen.getByLabelText('Ghi chú quyền sử dụng'), 'Owned fixture')
    await user.upload(screen.getByLabelText('Tệp tài liệu'), new File(['text'], 'guide.txt', { type: 'text/plain' }))
    fireEvent.submit(screen.getByRole('button', { name: 'Tải lên chờ duyệt' }).closest('form'))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3))
    expect(await screen.findByRole('alert')).toHaveTextContent('RAG service unavailable')
  })

  test('keeps unlock controls keyboard reachable', async () => {
    const user = userEvent.setup()
    renderApp('/admin/rag', null)
    await user.click(screen.getByRole('button', { name: 'Bật đèn bàn học' }))
    for (let index = 0; index < 12 && !screen.getByRole('textbox', { name: 'Email' }).matches(':focus'); index += 1) await user.tab()
    expect(screen.getByRole('textbox', { name: 'Email' })).toHaveFocus()
  })
})
