import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import App from '../App'

describe('writing assessment route', () => {
  test('exposes a Task 1/2 editor with an explicit estimated-band boundary', () => {
    render(
      <MemoryRouter initialEntries={['/practice/writing']}>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Writing practice' })).toBeInTheDocument()
    expect(screen.getByLabelText('Bài viết')).toBeInTheDocument()
    expect(screen.getByText(/Band ước lượng/i)).toBeInTheDocument()
  })
})
