import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, test, vi } from 'vitest'
import ListeningPracticeWorkspace from '../components/listening/ListeningPracticeWorkspace'

const questions = [{ id: 'listening-q1', prompt: 'What time does the library open?', options: ['7:30', '8:00'] }]

describe('truthful Listening workspace', () => {
  test('shows unavailable media without fabricating transcript or audio controls', () => {
    render(<ListeningPracticeWorkspace questions={questions} />)

    expect(screen.getByText(/phát audio chưa được cấu hình/i)).toBeInTheDocument()
    expect(screen.getByText(/chưa có media được phê duyệt/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /phát audio|nghe lại/i })).not.toBeInTheDocument()
    expect(screen.queryByText(/transcript/i)).not.toBeInTheDocument()
  })

  test('keeps objective answers keyboard reachable', async () => {
    const user = userEvent.setup()
    const onAnswer = vi.fn()
    render(<ListeningPracticeWorkspace questions={questions} onAnswer={onAnswer} />)
    await user.click(screen.getByLabelText('B. 8:00'))
    expect(onAnswer).toHaveBeenCalledWith('listening-q1', 'B')
  })
})
