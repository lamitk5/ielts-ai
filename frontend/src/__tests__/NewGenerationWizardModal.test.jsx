import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, test, vi } from 'vitest'
import NewGenerationWizardModal from '../components/admin/generator/NewGenerationWizardModal'

describe('NewGenerationWizardModal', () => {
  const mockSources = [
    { id: 'src-1', title: 'Climate Change In The Arctic', rightsStatus: 'APPROVED', checksum: 'abc1234567' },
    { id: 'src-2', title: 'Unauthorized Book Excerpt', rightsStatus: 'RESTRICTED', checksum: 'xyz9876543' },
  ]
  const mockBlueprints = [
    { id: 'bp-1', title: 'Reading Passage 1 Blueprint', skill: 'READING', targetBand: '6.5' },
    { id: 'bp-2', title: 'Reading Passage 2 Academic', skill: 'READING', targetBand: '7.5' },
  ]

  test('does not render when isOpen is false', () => {
    const { container } = render(<NewGenerationWizardModal isOpen={false} />)
    expect(container.firstChild).toBeNull()
  })

  test('allows selecting APPROVED source and proceeding through wizard', async () => {
    const onCreateJob = vi.fn().mockResolvedValue({ jobId: 'j-1' })
    const onClose = vi.fn()

    render(
      <NewGenerationWizardModal
        isOpen={true}
        onClose={onClose}
        sources={mockSources}
        blueprints={mockBlueprints}
        onCreateJob={onCreateJob}
      />
    )

    expect(screen.getByText('Khởi tạo đề thi IELTS AI mới')).toBeInTheDocument()

    // Step 1: Select approved source
    const approvedSource = screen.getByText('Climate Change In The Arctic')
    fireEvent.click(approvedSource)

    // Step 2: Choose blueprint
    expect(await screen.findByText('Reading Passage 2 Academic')).toBeInTheDocument()
    const bp2 = screen.getByText('Reading Passage 2 Academic')
    fireEvent.click(bp2)

    // Step 3: Confirmation
    expect(await screen.findByText('Sẵn sàng tạo bài với cấu hình:')).toBeInTheDocument()
    expect(screen.getByText('Band 7.5')).toBeInTheDocument()

    // Click launch
    const launchBtn = screen.getByRole('button', { name: /Bắt đầu tạo bài AI/i })
    fireEvent.click(launchBtn)

    await waitFor(() => {
      expect(onCreateJob).toHaveBeenCalledWith(
        expect.objectContaining({
          sourceId: 'src-1',
          blueprintId: 'bp-2',
        })
      )
      expect(onClose).toHaveBeenCalled()
    })
  })
})
