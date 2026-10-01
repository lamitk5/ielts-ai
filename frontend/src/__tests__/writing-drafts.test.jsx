import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import App from '../App'

const renderWriting = () => render(
  <MemoryRouter initialEntries={['/practice/writing']}>
    <App />
  </MemoryRouter>,
)

const memberSession = {
  token: 'valid-token',
  user: { id: 'u1', email: 'user@example.com' },
}

beforeEach(() => {
  localStorage.clear()
  localStorage.setItem('ielts-ai-tutor.session', JSON.stringify(memberSession))
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  localStorage.clear()
})

describe('Writing draft persistence and autosave', () => {
  test('hydrates active writing draft on mount for authenticated member', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((url) => {
      if (url.includes('/api/learning/drafts/current')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            id: 'draft-1',
            skill: 'WRITING',
            referenceId: 'task-1-academic-01',
            contentSnapshot: 'Existing drafted text for Task 1.',
            version: 2,
            status: 'ACTIVE',
          }),
        })
      }
      if (url === '/api/practice/writing/submissions') {
        return Promise.resolve({ ok: true, json: async () => [] })
      }
      return Promise.reject(new Error(`Unhandled: ${url}`))
    }))

    renderWriting()

    const textarea = await screen.findByRole('textbox', { name: 'Bài viết' })
    expect(textarea).toHaveValue('Existing drafted text for Task 1.')
    expect(screen.getByText('Đã lưu nháp')).toBeInTheDocument()
  })

  test('debounces draft save on typing and updates status indicator', async () => {
    let savedContent = ''
    vi.stubGlobal('fetch', vi.fn().mockImplementation((url, options) => {
      if (url.includes('/api/learning/drafts/current')) {
        return Promise.resolve({ ok: false, status: 404, json: async () => ({ error: 'NOT_FOUND' }) })
      }
      if (url.includes('/api/learning/drafts') && options?.method === 'PUT') {
        const body = JSON.parse(options.body)
        savedContent = body.contentSnapshot
        return Promise.resolve({
          ok: true,
          json: async () => ({
            id: 'draft-1',
            skill: 'WRITING',
            referenceId: 'task-1-academic-01',
            contentSnapshot: body.contentSnapshot,
            version: (body.expectedVersion ?? 0) + 1,
            status: 'ACTIVE',
          }),
        })
      }
      if (url === '/api/practice/writing/submissions') {
        return Promise.resolve({ ok: true, json: async () => [] })
      }
      return Promise.reject(new Error(`Unhandled: ${url}`))
    }))

    const user = userEvent.setup()
    renderWriting()

    const textarea = await screen.findByRole('textbox', { name: 'Bài viết' })
    await user.type(textarea, 'Drafting a new sentence.')

    expect(screen.getByText(/Đang lưu nháp|Chưa lưu/i)).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByText('Đã lưu nháp')).toBeInTheDocument()
    }, { timeout: 3000 })

    expect(savedContent).toBe('Drafting a new sentence.')
  })

  test('handles version conflict gracefully without clobbering editor input', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((url, options) => {
      if (url.includes('/api/learning/drafts/current')) {
        return Promise.resolve({ ok: false, status: 404, json: async () => ({ error: 'NOT_FOUND' }) })
      }
      if (url.includes('/api/learning/drafts') && options?.method === 'PUT') {
        return Promise.resolve({
          ok: false,
          status: 409,
          json: async () => ({
            error: { code: 'VERSION_CONFLICT', message: 'Bản nháp đã được chỉnh sửa từ thiết bị khác.' },
          }),
        })
      }
      if (url === '/api/practice/writing/submissions') {
        return Promise.resolve({ ok: true, json: async () => [] })
      }
      return Promise.reject(new Error(`Unhandled: ${url}`))
    }))

    const user = userEvent.setup()
    renderWriting()

    const textarea = await screen.findByRole('textbox', { name: 'Bài viết' })
    await user.type(textarea, 'Some conflicting text.')

    await waitFor(() => {
      expect(screen.getByText(/Lưu nháp thất bại|Xung đột bản nháp/i)).toBeInTheDocument()
    }, { timeout: 3000 })

    expect(textarea).toHaveValue('Some conflicting text.')
  })

  test('clears in-memory draft state on task switch and loads corresponding draft', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((url) => {
      if (url.includes('referenceId=task-1-academic-01')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            id: 'draft-1',
            skill: 'WRITING',
            referenceId: 'task-1-academic-01',
            contentSnapshot: 'Task 1 content',
            version: 1,
            status: 'ACTIVE',
          }),
        })
      }
      if (url.includes('referenceId=task-2-opinion-01')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            id: 'draft-2',
            skill: 'WRITING',
            referenceId: 'task-2-opinion-01',
            contentSnapshot: 'Task 2 content',
            version: 1,
            status: 'ACTIVE',
          }),
        })
      }
      if (url === '/api/practice/writing/submissions') {
        return Promise.resolve({ ok: true, json: async () => [] })
      }
      return Promise.reject(new Error(`Unhandled: ${url}`))
    }))

    const user = userEvent.setup()
    renderWriting()

    const textarea = await screen.findByRole('textbox', { name: 'Bài viết' })
    expect(textarea).toHaveValue('Task 1 content')

    const taskSelect = screen.getByRole('combobox', { name: /chọn dạng bài/i })
    await user.selectOptions(taskSelect, 'task-2-opinion-01')

    await waitFor(() => {
      expect(screen.getByRole('textbox', { name: 'Bài viết' })).toHaveValue('Task 2 content')
    })
  })
})
