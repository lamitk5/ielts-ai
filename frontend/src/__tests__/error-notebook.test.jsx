import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ErrorNotebookPage from '../pages/ErrorNotebookPage'

describe('error notebook', () => {
  it('distinguishes one observation from a repeated issue', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ entries: [{ id: 'm1', skill: 'READING', mistakeType: 'Detail', evidence: 'Bằng chứng từ bài đã nộp.', learnerAnswer: 'A', correctAnswer: 'B', repeated: false, repeatCount: 1 }] }) }))
    render(<MemoryRouter><ErrorNotebookPage /></MemoryRouter>)
    await waitFor(() => expect(screen.getByText('Một quan sát')).toBeInTheDocument())
    expect(screen.getByText(/Bằng chứng từ bài/)).toBeInTheDocument()
  })
})
