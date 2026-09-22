import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import { createRagAdminApi, readAdminToken, writeAdminToken } from '../services/ragAdminApi'

function renderApp(initialEntry = '/admin/rag') {
  return render(<MemoryRouter initialEntries={[initialEntry]}><App /></MemoryRouter>)
}

beforeEach(() => {
  window.sessionStorage.clear()
  vi.restoreAllMocks()
})

describe('temporary RAG admin CMS', () => {
  test('renders unlock without token', () => {
    renderApp()
    expect(screen.getByRole('heading', { name: /mở khóa quản trị học liệu/i })).toBeInTheDocument()
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
    expect(screen.getAllByText('INDEXED').length).toBeGreaterThan(0)
  })

  test('handles 401 and 403', async () => {
    const fetchImpl = vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ error: { code: 'RAG_ADMIN_UNAUTHORIZED' } }) })
    const api = createRagAdminApi({ fetchImpl, storage: { getItem: () => 'secret' } })
    await expect(api.list()).rejects.toMatchObject({ status: 401, code: 'RAG_ADMIN_UNAUTHORIZED' })
  })

  test('keeps unlock controls keyboard reachable', async () => {
    const user = userEvent.setup()
    renderApp()
    for (let index = 0; index < 12 && !screen.getByLabelText(/admin token/i).matches(':focus'); index += 1) await user.tab()
    expect(screen.getByLabelText(/admin token/i)).toHaveFocus()
  })
})
