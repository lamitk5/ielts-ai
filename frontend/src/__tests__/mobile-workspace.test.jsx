import { describe, expect, test, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MobileWorkspaceTabs } from '../components/workspace/MobileWorkspaceTabs'

describe('Task 8: Mobile Workspace Tabs & Scroll Preservation', () => {
  test('renders mobile tablist and switches tabs on click', async () => {
    const user = userEvent.setup()
    const onTabChange = vi.fn()

    render(
      <MobileWorkspaceTabs
        leftLabel="Đoạn văn"
        rightLabel="Câu hỏi"
        left={<p>Nội dung đoạn văn</p>}
        right={<p>Nội dung câu hỏi</p>}
        forceMobile={true}
        onTabChange={onTabChange}
      />
    )

    const tabList = screen.getByRole('tablist', { name: /Khung học tập/i })
    expect(tabList.closest('.workspace-mobile')).toHaveClass('workspace-responsive-safe')
    expect(tabList).toBeInTheDocument()

    const leftTab = screen.getByRole('tab', { name: /Đoạn văn/i })
    const rightTab = screen.getByRole('tab', { name: /Câu hỏi/i })

    expect(leftTab).toHaveAttribute('aria-selected', 'true')
    expect(rightTab).toHaveAttribute('aria-selected', 'false')

    await user.click(rightTab)
    expect(onTabChange).toHaveBeenCalledWith(1)
  })

  test('supports keyboard navigation between mobile tabs', async () => {
    const user = userEvent.setup()
    const onTabChange = vi.fn()

    render(
      <MobileWorkspaceTabs
        leftLabel="Đề bài"
        rightLabel="Bài làm"
        left={<p>Nội dung đề bài</p>}
        right={<p>Nội dung bài làm</p>}
        forceMobile={true}
        onTabChange={onTabChange}
      />
    )

    const leftTab = screen.getByRole('tab', { name: /Đề bài/i })
    leftTab.focus()

    await user.keyboard('{ArrowRight}')
    expect(onTabChange).toHaveBeenCalledWith(1)
  })
})
