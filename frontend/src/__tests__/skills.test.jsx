import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import App from '../App'
import { skillCards } from '../data/homepageMockData'

function renderApp(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <App />
    </MemoryRouter>
  )
}

describe('four-skill cards section', () => {
  test('renders the section identity and all four first-class skills', () => {
    renderApp()

    const section = screen.getByRole('region', { name: 'Luyện tập theo 4 kỹ năng' })

    expect(section).toHaveAttribute('id', 'skills')
    expect(within(section).getByText('4 KỸ NĂNG IELTS')).toBeInTheDocument()
    expect(within(section).getByRole('heading', { name: 'Luyện tập theo 4 kỹ năng' })).toBeInTheDocument()

    for (const skill of ['Reading', 'Listening', 'Writing', 'Speaking']) {
      expect(within(section).getByRole('heading', { name: skill })).toBeInTheDocument()
    }
  })

  test('links each skill card to its matching practice route', () => {
    renderApp()

    const expectedRoutes = {
      Reading: '/practice/reading',
      Listening: '/practice/listening',
      Writing: '/practice/writing',
      Speaking: '/practice/speaking',
    }

    for (const [skill, route] of Object.entries(expectedRoutes)) {
      const card = screen.getByRole('article', { name: skill })
      expect(within(card).getByRole('link', { name: /Luyện tập/ })).toHaveAttribute('href', route)
    }
  })

  test('gives every card the same complete and keyboard-accessible structure', () => {
    renderApp()

    const cards = screen.getAllByRole('article')
    expect(cards).toHaveLength(4)
    expect(new Set(cards.map((card) => card.className)).size).toBe(1)

    for (const skill of skillCards) {
      const card = screen.getByRole('article', { name: skill.name })
      const action = within(card).getByRole('link', { name: /Luyện tập/ })

      expect(within(card).getByRole('heading')).toBeInTheDocument()
      expect(within(card).getByText(skill.description)).toBeInTheDocument()
      expect(within(card).getByText(skill.metric)).toBeInTheDocument()
      expect(action).toHaveAttribute('href', `/practice/${skill.id}`)
      action.focus()
      expect(action).toHaveFocus()
    }
  })
})
