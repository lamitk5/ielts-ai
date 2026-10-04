import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import PracticeCatalogPage from '../pages/PracticeCatalogPage'

beforeEach(() => { vi.stubGlobal('fetch', vi.fn()) })
afterEach(() => { vi.unstubAllGlobals() })

describe('learner practice catalog', () => {
  test('renders active catalog cards for all four skills with keyboard-accessible routes', async () => {
    fetch.mockImplementation(async (url) => {
      if (url.includes('/reading/sets')) return { ok: true, json: async () => [{ id: 'reading-1', skill: 'Reading', title: 'Reading set', description: 'Read well', active: true }] }
      if (url.includes('/listening/sets')) return { ok: true, json: async () => [{ id: 'listening-1', skill: 'Listening', title: 'Listening set', description: 'Listen well', active: true }] }
      return { ok: true, json: async () => [] }
    })

    render(<MemoryRouter><PracticeCatalogPage /></MemoryRouter>)

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Luyện tập theo 4 kỹ năng' })).toBeInTheDocument())
    for (const skill of ['Reading', 'Listening', 'Writing', 'Speaking']) {
      expect(screen.getByRole('heading', { name: skill })).toBeInTheDocument()
    }
    expect(screen.getByRole('link', { name: /Reading/i })).toHaveAttribute('href', '/practice/reading/reading-1')
    expect(screen.getByRole('link', { name: /Listening/i })).toHaveAttribute('href', '/practice/listening/listening-1')
    expect(screen.getByRole('link', { name: /Writing/i })).toHaveAttribute('href', '/practice/writing')
    expect(screen.getByRole('link', { name: /Speaking/i })).toHaveAttribute('href', '/practice/speaking')
  })
})
