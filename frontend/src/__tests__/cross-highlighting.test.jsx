import { describe, expect, test, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TutorReferenceRegistry } from '../features/tutor/TutorReferenceRegistry'
import { TutorReferenceResolver } from '../features/tutor/TutorReferenceResolver'
import { CrossHighlightLayer } from '../components/tutor/CrossHighlightLayer'
import TutorMessage from '../components/tutor/TutorMessage'
import { ReadingPassagePane } from '../components/workspace/ReadingPassagePane'
import { ReadingQuestionPane } from '../components/workspace/ReadingQuestionPane'
import { WritingEditorPane } from '../components/workspace/WritingEditorPane'

describe('Task 6: Tutor Reference Registry & Resolver', () => {
  let registry

  beforeEach(() => {
    registry = new TutorReferenceRegistry()
  })

  afterEach(() => {
    registry.clear()
  })

  test('registers and retrieves targets by safe ID, rejects invalid IDs', () => {
    const el = document.createElement('div')
    registry.register({ targetId: 'passage-p1', type: 'PASSAGE', element: el })

    expect(registry.getTarget('passage-p1')).toEqual({
      targetId: 'passage-p1',
      type: 'PASSAGE',
      element: el,
    })

    // Unsafe ID rejected
    expect(() => registry.register({ targetId: '<script>evil</script>', type: 'PASSAGE', element: el })).toThrow()

    // Unregister
    registry.unregister('passage-p1')
    expect(registry.getTarget('passage-p1')).toBeUndefined()
  })

  test('resolves active immutable Reading passage reference', () => {
    const el = document.createElement('div')
    registry.register({ targetId: 'passage-p1', type: 'PASSAGE', element: el })

    const reference = {
      referenceType: 'PASSAGE',
      targetId: 'passage-p1',
      paragraphId: 'p1',
      label: 'Đoạn 1',
    }

    const result = TutorReferenceResolver.resolve(reference, registry, { skill: 'reading' })
    expect(result.status).toBe('ACTIVE')
    expect(result.target.element).toBe(el)
  })

  test('resolves DRAFT reference when draftVersion matches current workspace draftVersion', () => {
    const el = document.createElement('div')
    registry.register({ targetId: 'writing-editor', type: 'DRAFT', element: el })

    const reference = {
      referenceType: 'DRAFT',
      targetId: 'writing-editor',
      draftVersion: 3,
      startOffset: 10,
      endOffset: 50,
      label: 'Ngữ pháp',
    }

    const result = TutorReferenceResolver.resolve(reference, registry, { draftVersion: 3 })
    expect(result.status).toBe('ACTIVE')
    expect(result.target.element).toBe(el)
  })

  test('returns STALE for DRAFT reference when draftVersion is outdated and does not approximate offsets', () => {
    const el = document.createElement('div')
    registry.register({ targetId: 'writing-editor', type: 'DRAFT', element: el })

    const reference = {
      referenceType: 'DRAFT',
      targetId: 'writing-editor',
      draftVersion: 2,
      startOffset: 10,
      endOffset: 50,
      label: 'Ngữ pháp',
    }

    // Current version is 4 (modified by learner)
    const result = TutorReferenceResolver.resolve(reference, registry, { draftVersion: 4 })
    expect(result.status).toBe('STALE')
    expect(result.reason).toBe('VERSION_MISMATCH')
    expect(result.target).toBeNull()
  })

  test('returns UNKNOWN_TARGET when referenced target is not in registry', () => {
    const reference = {
      referenceType: 'QUESTION',
      targetId: 'non-existent-q99',
      label: 'Câu 99',
    }

    const result = TutorReferenceResolver.resolve(reference, registry, { skill: 'reading' })
    expect(result.status).toBe('UNKNOWN_TARGET')
    expect(result.target).toBeNull()
  })
})

describe('Task 6: Cross-Highlighting UI & Accessibility', () => {
  let registry
  let mockScrollIntoView

  beforeEach(() => {
    registry = new TutorReferenceRegistry()
    mockScrollIntoView = vi.fn()
    window.HTMLElement.prototype.scrollIntoView = mockScrollIntoView
  })

  afterEach(() => {
    registry.clear()
    vi.restoreAllMocks()
  })

  test('TutorMessage renders reference chips and clicking activates target highlight and scroll', async () => {
    const user = userEvent.setup()
    const targetEl = document.createElement('div')
    targetEl.setAttribute('data-reading-target-id', 'passage-p1')
    document.body.appendChild(targetEl)
    registry.register({ targetId: 'passage-p1', type: 'PASSAGE', element: targetEl })

    const onActivate = vi.fn()
    const onPreview = vi.fn()

    const message = {
      role: 'assistant',
      content: 'Hãy chú ý đoạn 1 của bài đọc.',
      references: [
        {
          referenceType: 'PASSAGE',
          targetId: 'passage-p1',
          label: 'Xem đoạn 1',
        },
      ],
    }

    render(
      <CrossHighlightLayer registry={registry} workspaceState={{ skill: 'reading' }}>
        <TutorMessage message={message} />
      </CrossHighlightLayer>
    )

    const chip = screen.getByRole('button', { name: /Xem đoạn 1/i })
    expect(chip).toBeInTheDocument()
    expect(chip).toHaveAttribute('data-reference-state', 'active')
    expect(chip).toHaveAttribute('aria-pressed', 'false')

    // Hover triggers preview
    await user.hover(chip)
    expect(targetEl.getAttribute('data-tutor-highlight')).toBe('preview')

    // Click triggers active highlight and scroll
    await user.click(chip)
    expect(targetEl.getAttribute('data-tutor-highlight')).toBe('active')
    expect(chip).toHaveAttribute('aria-pressed', 'true')
    expect(chip).toHaveClass('is-active')
    expect(mockScrollIntoView).toHaveBeenCalled()

    // Pressing Escape clears active highlight
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(targetEl.getAttribute('data-tutor-highlight')).toBeNull()

    document.body.removeChild(targetEl)
  })

  test('stale Writing draft reference renders stale badge and does not apply DOM highlight', async () => {
    const user = userEvent.setup()
    const targetEl = document.createElement('div')
    targetEl.setAttribute('data-writing-target-id', 'writing-editor')
    document.body.appendChild(targetEl)
    registry.register({ targetId: 'writing-editor', type: 'DRAFT', element: targetEl })

    const onReanalyze = vi.fn()
    const message = {
      role: 'assistant',
      content: 'Câu mở đầu đoạn 2 cần dùng từ nối tự nhiên hơn.',
      references: [
        {
          referenceType: 'DRAFT',
          targetId: 'writing-editor',
          draftVersion: 1,
          startOffset: 20,
          endOffset: 60,
          label: 'Gợi ý từ nối',
        },
      ],
    }

    render(
      <CrossHighlightLayer registry={registry} workspaceState={{ draftVersion: 3 }} onReanalyzeDraft={onReanalyze}>
        <TutorMessage message={message} />
      </CrossHighlightLayer>
    )

    // Stale indicator is shown
    expect(screen.getByText(/bản nháp cũ/i)).toBeInTheDocument()

    // Stale chip click does not set active highlight
    const chip = screen.getByRole('button', { name: /Gợi ý từ nối/i })
    expect(chip).toHaveAttribute('data-reference-state', 'stale')
    expect(chip).toHaveAttribute('aria-disabled', 'true')
    await user.click(chip)
    expect(targetEl.getAttribute('data-tutor-highlight')).toBeNull()

    // Shows Re-analyze action
    const reanalyzeBtn = screen.getByRole('button', { name: /Phân tích lại bản nháp/i })
    expect(reanalyzeBtn).toBeInTheDocument()
    await user.click(reanalyzeBtn)
    expect(onReanalyze).toHaveBeenCalledTimes(1)

    document.body.removeChild(targetEl)
  })

  test('ReadingPassagePane registers its paragraphs into registry on mount', () => {
    const practiceSet = {
      id: 'set-1',
      setId: 'set-1',
      title: 'Passage Title',
      passage: {
        paragraphs: [
          { id: 'p1', text: 'Paragraph 1 text' },
          { id: 'p2', text: 'Paragraph 2 text' },
        ],
      },
    }

    render(<ReadingPassagePane practiceSet={practiceSet} registry={registry} />)
    expect(registry.getTarget('p1')).toBeDefined()
    expect(registry.getTarget('p2')).toBeDefined()
  })

  test('ReadingQuestionPane registers current question into registry on mount', () => {
    const questions = [
      { id: 'q1', prompt: 'Question 1', options: ['A', 'B'] },
      { id: 'q2', prompt: 'Question 2', options: ['C', 'D'] },
    ]

    render(
      <ReadingQuestionPane
        questions={questions}
        currentQuestionId="q1"
        answers={{}}
        flaggedIds={new Set()}
        reviewedIds={new Set()}
        onSelect={vi.fn()}
        onAnswer={vi.fn()}
        onToggleFlag={vi.fn()}
        onToggleReviewed={vi.fn()}
        registry={registry}
      />
    )

    expect(registry.getTarget('q1')).toBeDefined()
  })

  test('WritingEditorPane registers editor into registry on mount', () => {
    render(<WritingEditorPane registry={registry} />)
    expect(registry.getTarget('writing-editor')).toBeDefined()
  })
})
