import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import PracticeCatalogPage from '../pages/PracticeCatalogPage'

beforeEach(() => { vi.stubGlobal('fetch', vi.fn()) })
afterEach(() => { vi.unstubAllGlobals() })

describe('student-ready learner flow', () => {
  test('shows only active approved catalog entries and preserves four-skill entry points', async () => {
    fetch.mockImplementation(async (url) => {
      const skill = url.split('/').at(-2)
      return { ok: true, json: async () => skill === 'reading'
        ? [{ id: 'inactive', active: false, title: 'Hidden' }, { id: 'reading-live', active: true, title: 'Reading live', skill: 'reading' }]
        : [] }
    })

    render(<MemoryRouter><PracticeCatalogPage /></MemoryRouter>)

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Luyện tập theo 4 kỹ năng' })).toBeInTheDocument())
    expect(screen.getByText('Reading live')).toBeInTheDocument()
    expect(screen.queryByText('Hidden')).not.toBeInTheDocument()
    for (const skill of ['Reading', 'Listening', 'Writing', 'Speaking']) {
      expect(screen.getByRole('heading', { name: skill })).toBeInTheDocument()
    }
  })
})
