import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test, vi } from 'vitest'
import App from '../App'

describe('deterministic practice routes', () => {
  test.each([
    ['/practice/reading', 'Reading practice'],
    ['/practice/listening', 'Listening practice'],
  ])('renders a usable %s shell', (route, heading) => {
    render(
      <MemoryRouter initialEntries={[route]}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: heading })).toBeInTheDocument()
    expect(screen.getByText(/synthetic practice set/i)).toBeInTheDocument()
  })

  test('states the safe audio boundary for the synthetic Listening fixture', () => {
    render(
      <MemoryRouter initialEntries={['/practice/listening']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByText(/phát audio chưa được cấu hình/i)).toBeInTheDocument()
  })

  test('normalizes the backend practice-set shape before rendering', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{
        id: 'reading-foundation-01',
        skill: 'Reading',
        title: 'Reading foundation',
        description: 'Một bộ đề synthetic.',
        questions: [{ id: 'reading-q1', prompt: 'What is the main purpose?', options: ['A', 'B'] }],
      }],
    }))

    render(
      <MemoryRouter initialEntries={['/practice/reading']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: 'Reading practice' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'What is the main purpose?' })).toBeInTheDocument()
  })

  test('unknown skill keeps a safe fallback', () => {
    render(
      <MemoryRouter initialEntries={['/practice/unknown']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: /Practice area unavailable/ })).toBeInTheDocument()
  })
})
