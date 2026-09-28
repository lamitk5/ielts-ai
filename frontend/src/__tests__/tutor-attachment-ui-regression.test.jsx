import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import FloatingTutor from '../components/tutor/FloatingTutor'
import { AuthProvider } from '../features/auth/AuthProvider'

function renderTutor() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <FloatingTutor />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('Én attachment upload lifecycle', () => {
  const originalFetch = global.fetch

  beforeEach(() => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
      token: 'test-token',
      user: { id: 'qa-user', email: 'qa@example.com' },
    }))
  })

  afterEach(() => {
    global.fetch = originalFetch
    localStorage.removeItem('ielts-ai-tutor.session')
  })

  test('renders the real 201 STORED → PROCESSING → READY attachment progression', async () => {
    const user = userEvent.setup()
    let attachmentPolls = 0
    let resolveUpload
    global.fetch = vi.fn(async (input, options = {}) => {
      const url = String(input)
      if (url.startsWith('/api/ai/conversations?') && options.method === 'POST') {
        return { ok: true, status: 201, json: async () => ({ id: 'conversation-test' }) }
      }
      if (url === '/api/ai/conversations') {
        return { ok: true, status: 200, json: async () => [] }
      }
      if (url === '/api/ai/attachments' && options.method === 'POST') {
        return new Promise((resolve) => {
          resolveUpload = () => resolve({
            ok: true,
            status: 201,
            json: async () => ({
              attachments: [{
                id: 'test-id',
                conversationId: 'conversation-test',
                filename: 'study-notes.jpg',
                contentType: 'image/jpeg',
                kind: 'IMAGE',
                sizeBytes: 4,
                status: 'STORED',
                errorCode: null,
              }],
            }),
          })
        })
      }
      if (url === '/api/ai/attachments/test-id') {
        attachmentPolls += 1
        const status = attachmentPolls === 1 ? 'PROCESSING' : 'READY'
        return {
          ok: true,
          status: 200,
          json: async () => ({
            id: 'test-id',
            conversationId: 'conversation-test',
            filename: 'study-notes.jpg',
            contentType: 'image/jpeg',
            kind: 'IMAGE',
            sizeBytes: 4,
            status,
            errorCode: null,
          }),
        }
      }
      throw new Error(`Unexpected request: ${url}`)
    })

    renderTutor()
    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument())

    const file = new File(['test'], 'study-notes.jpg', { type: 'image/jpeg' })
    await user.upload(screen.getByLabelText('Chọn tệp tải lên'), file)

    await waitFor(() => expect(global.fetch).toHaveBeenCalledWith('/api/ai/attachments', expect.objectContaining({ method: 'POST' })))
    expect(screen.getByText('Đang tải lên...')).toBeInTheDocument()
    resolveUpload()
    await waitFor(() => expect(screen.getByText('Đang đọc nội dung...')).toBeInTheDocument())
    await waitFor(() => expect(screen.getByText('Sẵn sàng')).toBeInTheDocument())
    expect(attachmentPolls).toBe(2)
  })

  test('renders a selected file before conversation creation and upload complete', async () => {
    const user = userEvent.setup()
    let resolveConversation
    global.fetch = vi.fn((input, options = {}) => {
      const url = String(input)
      if (url.startsWith('/api/ai/conversations?') && options.method === 'POST') {
        return new Promise((resolve) => {
          resolveConversation = () => resolve({ ok: true, status: 201, json: async () => ({ id: 'conversation-pending' }) })
        })
      }
      if (url === '/api/ai/conversations') {
        return Promise.resolve({ ok: true, status: 200, json: async () => [] })
      }
      throw new Error(`Unexpected request before conversation is ready: ${url}`)
    })

    renderTutor()
    await user.click(screen.getByRole('button', { name: 'Mở Én' }))
    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument())

    await user.upload(
      screen.getByLabelText('Chọn tệp tải lên'),
      new File(['test'], 'queued-before-conversation.txt', { type: 'text/plain' }),
    )

    expect(screen.getByText('queued-before-conversation.txt')).toBeInTheDocument()
    expect(screen.getByText('Đang kiểm tra...')).toBeInTheDocument()
    expect(global.fetch).not.toHaveBeenCalledWith('/api/ai/attachments', expect.anything())

    resolveConversation()
    await waitFor(() => expect(global.fetch).toHaveBeenCalledWith('/api/ai/attachments', expect.objectContaining({ method: 'POST' })))
  })
})
