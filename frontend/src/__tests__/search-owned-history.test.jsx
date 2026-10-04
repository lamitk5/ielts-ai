import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import SearchPage from '../pages/SearchPage'

describe('SearchPage owned history and categories', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  test('renders search results with category badges and filters', async () => {
    const mockResults = [
      {
        id: 'reading-01',
        title: 'Reading Test 1',
        skill: 'Reading',
        description: 'Practice reading skills',
        route: '/practice/reading/reading-01',
        resultType: 'PRACTICE_SET',
        typeLabel: 'Bài luyện',
      },
      {
        id: 'sub-123',
        title: 'Lịch sử bài làm: Writing (task-1)',
        skill: 'Writing',
        description: 'Trạng thái: SUBMITTED',
        route: '/practice/results/sub-123',
        resultType: 'SUBMISSION_HISTORY',
        typeLabel: 'Lịch sử bài làm',
      },
    ]

    global.fetch = vi.fn().mockImplementation((url) => {
      if (url.includes('/api/practice/search')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockResults),
        })
      }
      if (url.includes('/status')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ saved: false }),
        })
      }
      return Promise.reject(new Error('not found'))
    })

    render(
      <MemoryRouter initialEntries={['/practice/search?q=test']}>
        <SearchPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Reading Test 1')).toBeInTheDocument()
    expect(screen.getByText('Lịch sử bài làm: Writing (task-1)')).toBeInTheDocument()
    expect(screen.getAllByText('Bài luyện').length).toBeGreaterThanOrEqual(1)
    expect(screen.getAllByText('Lịch sử bài làm').length).toBeGreaterThanOrEqual(1)
  })
})
