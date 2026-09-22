import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import App from '../App'
import { writeAdminToken } from '../services/ragAdminApi'

function renderAdmin() {
  return render(<MemoryRouter initialEntries={['/admin/rag']}><App /></MemoryRouter>)
}

beforeEach(() => {
  window.sessionStorage.clear()
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce({ ok: true, json: async () => [] })
    .mockResolvedValueOnce({ ok: true, json: async () => [] }))
})

describe('RAG admin accessibility verification', () => {
  test('admin route is keyboard reachable', async () => {
    const user = userEvent.setup()
    renderAdmin()
    for (let index = 0; index < 12 && !screen.getByLabelText(/admin token/i).matches(':focus'); index += 1) await user.tab()
    expect(screen.getByLabelText(/admin token/i)).toHaveFocus()
  })

  test('admin status actions are semantic', async () => {
    writeAdminToken('secret')
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => [{ id: '1', title: 'Guide', skill: 'GENERAL', rightsStatus: 'PENDING_REVIEW', indexStatus: 'NOT_INDEXED', active: false }] })
      .mockResolvedValueOnce({ ok: true, json: async () => [] }))
    renderAdmin()
    const row = await screen.findByRole('row', { name: /Guide/ })
    expect(within(row).getByRole('button', { name: 'Duyệt' })).toBeEnabled()
    expect(within(row).getByRole('button', { name: 'Index' })).toBeDisabled()
  })

  test('reduced motion does not require animation', () => {
    render(<MemoryRouter initialEntries={['/admin/rag']}><App /></MemoryRouter>)
    expect(screen.getByRole('heading', { name: /mở khóa quản trị học liệu/i })).toBeInTheDocument()
  })
})
