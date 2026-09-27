import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import FloatingTutor from '../components/tutor/FloatingTutor'

const base = { status: 'ANSWERED', grounding: { status: 'GROUNDED', ragEnabled: true }, meta: {}, timestamp: '' }

beforeEach(() => { global.fetch = vi.fn() })
afterEach(() => { vi.restoreAllMocks() })

async function send(response) {
  global.fetch.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ ...base, ...response }) })
  const user = userEvent.setup()
  render(<FloatingTutor />)
  await user.click(screen.getByRole('button', { name: 'Mở Én' }))
  await waitFor(() => expect(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' })).toBeInTheDocument())
  await user.type(screen.getByRole('textbox', { name: 'Tin nhắn cho Én' }), 'Explain this rubric')
  await user.keyboard('{Enter}')
  return waitFor(() => screen.getByText(response.answer))
}

describe('Tutor RAG citations', () => {
  test('opens a viewport-safe Tutor shell for mobile-sized surfaces', async () => {
    const user = userEvent.setup()
    render(<FloatingTutor />)
    await user.click(screen.getByRole('button', { name: 'Mở Én' }))

    await waitFor(() => expect(screen.getByRole('dialog', { name: 'Én' })).toBeInTheDocument())
    expect(screen.getByRole('dialog', { name: 'Én' })).toHaveClass('tutor-shell-viewport-safe')
  })

  test('renders real title, section and page citation', async () => {
    await send({ answer: 'Grounded answer', sources: [{ sourceId: 'guide-1', title: 'Writing Guide', section: 'Task Response', page: 4, version: 'v1', chunkId: 'chunk-1' }] })
    expect(screen.getByText(/Writing Guide/)).toBeInTheDocument()
    expect(screen.getByText(/Task Response/)).toBeInTheDocument()
    expect(screen.getByText(/p\. 4/)).toBeInTheDocument()
  })

  test('renders multiple grounded sources', async () => {
    await send({ answer: 'Two sources', sources: [{ sourceId: 'one', title: 'One', section: 'A' }, { sourceId: 'two', title: 'Two', section: 'B' }] })
    expect(screen.getByText('One')).toBeInTheDocument()
    expect(screen.getByText('Two')).toBeInTheDocument()
  })

  test('renders no chip for empty sources', async () => {
    await send({ answer: 'No source answer', sources: [] })
    expect(screen.queryByRole('list', { name: 'Nguồn tham chiếu' })).not.toBeInTheDocument()
  })

  test('renders insufficient evidence without invented source', async () => {
    await send({ status: 'INSUFFICIENT_CONTEXT', answer: 'Chưa tìm thấy nguồn đủ phù hợp.', sources: [], grounding: { status: 'INSUFFICIENT_EVIDENCE', ragEnabled: true } })
    expect(screen.getByText('Cần thêm ngữ cảnh')).toBeInTheDocument()
    expect(screen.queryByText(/Writing Guide/)).not.toBeInTheDocument()
  })

  test('keeps provider-neutral insufficient evidence state without provider payloads', async () => {
    await send({ status: 'INSUFFICIENT_EVIDENCE', answer: 'Chưa có đủ bằng chứng từ nguồn đã duyệt.', sources: [], grounding: { status: 'INSUFFICIENT_EVIDENCE', ragEnabled: true }, providerResponse: { raw: 'hidden' } })
    expect(screen.getByText('Chưa có đủ bằng chứng từ nguồn đã duyệt.')).toBeInTheDocument()
    expect(screen.getByText('Cần thêm ngữ cảnh')).toBeInTheDocument()
    expect(screen.queryByText(/hidden|providerResponse/)).not.toBeInTheDocument()
  })

  test('ignores unsupported provider fields', async () => {
    await send({ answer: 'Normalized answer', sources: [{ sourceId: 'safe', title: 'Safe', section: 'Section', similarity: 0.99, providerPayload: { secret: 'do not render' } }] })
    expect(screen.getByText(/Safe/)).toBeInTheDocument()
    expect(screen.queryByText(/0\.99|secret|providerPayload/)).not.toBeInTheDocument()
  })

  test('renders every sentence in a long grounded answer', async () => {
    const answer = 'First sentence. Second sentence. Third sentence.'
    await send({ answer, sources: [] })
    expect(screen.getByText(answer)).toHaveTextContent('First sentence. Second sentence. Third sentence.')
  })
})
