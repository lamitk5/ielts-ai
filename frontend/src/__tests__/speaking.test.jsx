import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import App from '../App'

describe('speaking practice boundary', () => {
  test('renders prompt flow and explicit unavailable transcription state', () => {
    render(
      <MemoryRouter initialEntries={['/practice/speaking']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Speaking practice' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Lưu câu trả lời' })).toBeInTheDocument()
    expect(screen.getByText(/STT chưa được cấu hình/i)).toBeInTheDocument()
  })
})
