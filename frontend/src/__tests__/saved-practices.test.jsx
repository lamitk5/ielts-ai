import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import SavedPracticesPage from '../pages/SavedPracticesPage'

describe('SavedPracticesPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  test('renders saved practice items with action links', async () => {
    const mockItems = [
      {
        id: 'saved-1',
        userId: 'user-1',
        publishedSetId: 'reading-foundation-01',
        skill: 'reading',
        title: 'Reading foundation',
        savedAt: '2026-10-01T10:00:00Z',
        available: true,
      },
    ]

    global.fetch = vi.fn().mockImplementation((url) => {
      if (url.includes('/api/me/saved-practices')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockItems),
        })
      }
      return Promise.reject(new Error('not found'))
    })

    render(
      <MemoryRouter>
        <SavedPracticesPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Reading foundation')).toBeInTheDocument()
    expect(screen.getByText('Luyện tập ngay')).toBeInTheDocument()
  })

  test('allows unsaving a practice with instant UI update', async () => {
    const mockItems = [
      {
        id: 'saved-1',
        userId: 'user-1',
        publishedSetId: 'reading-foundation-01',
        skill: 'reading',
        title: 'Reading foundation',
        savedAt: '2026-10-01T10:00:00Z',
        available: true,
      },
    ]

    global.fetch = vi.fn().mockImplementation((url, options) => {
      if (options?.method === 'DELETE') {
        return Promise.resolve({ ok: true, status: 204 })
      }
      if (url.includes('/api/me/saved-practices')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockItems),
        })
      }
      return Promise.reject(new Error('not found'))
    })

    render(
      <MemoryRouter>
        <SavedPracticesPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Reading foundation')).toBeInTheDocument()
    const deleteButton = screen.getByRole('button', { name: /Bỏ lưu/i })
    fireEvent.click(deleteButton)

    await waitFor(() => {
      expect(screen.getByText('Chưa có bài luyện nào được lưu')).toBeInTheDocument()
    })
  })
})
