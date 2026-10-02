import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import DiagnosticResult from '../components/diagnostic/DiagnosticResult'
import DiagnosticStepper from '../components/diagnostic/DiagnosticStepper'

describe('diagnostic journey', () => {
  it('uses honest estimated starting profile language', () => {
    render(<DiagnosticResult result={{ title: 'Estimated starting profile', disclaimer: 'Không phải kết quả IELTS chính thức.', sections: [{ skill: 'READING', state: 'INSUFFICIENT_EVIDENCE' }] }} />)
    expect(screen.getByText('Estimated starting profile')).toBeInTheDocument()
    expect(screen.getByText(/không phải kết quả IELTS chính thức/i)).toBeInTheDocument()
    expect(screen.getByText('Insufficient evidence')).toBeInTheDocument()
  })

  it('renders all four sections and accessible unavailable actions', () => {
    render(<DiagnosticStepper session={{ id: 's1', state: 'IN_PROGRESS' }} sections={[]} onUnavailable={() => {}} />)
    expect(screen.getAllByRole('button', { name: /đánh dấu chưa khả dụng/i })).toHaveLength(4)
    expect(screen.getByRole('button', { name: /hoàn tất đánh giá/i })).toBeInTheDocument()
  })
})
