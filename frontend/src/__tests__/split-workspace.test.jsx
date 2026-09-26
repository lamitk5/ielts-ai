import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, test, vi } from 'vitest'
import { PreferenceProvider } from '../features/preferences/PreferenceProvider'
import { isCompletePreferences, normalizePreferences, WORKSPACE_RATIO_PRESETS } from '../features/preferences/preferenceSchema'
import { DEFAULT_PREFERENCES } from '../features/preferences/preferenceDefaults'
import { getWorkspacePreferenceKey, getWorkspaceRatio, saveWorkspaceRatio } from '../features/workspace/workspacePreferences'
import { SplitLearningWorkspace } from '../components/workspace/SplitLearningWorkspace'
import { WorkspaceDivider } from '../components/workspace/WorkspaceDivider'
import { WorkspacePresetControls } from '../components/workspace/WorkspacePresetControls'
import { MobileWorkspaceTabs } from '../components/workspace/MobileWorkspaceTabs'

function WorkspaceProbe() {
  return (
    <PreferenceProvider>
      <SplitLearningWorkspace
        left={<div>Passage content</div>}
        right={<div>Question content</div>}
        leftLabel="Nội dung"
        rightLabel="Câu hỏi"
        mobileMode
      />
    </PreferenceProvider>
  )
}

describe('split learning workspace', () => {
  test.each([40, 50, 60])('supports the %i pane preset', (ratio) => {
    const onChange = vi.fn()
    render(<WorkspacePresetControls value={ratio} onChange={onChange} />)
    expect(screen.getByRole('button', { name: `${ratio}/${100 - ratio}` })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: `${ratio}/${100 - ratio}` })).toBeInTheDocument()
  })

  test('renders separately scrollable named pane regions', () => {
    render(<WorkspaceProbe />)
    expect(screen.getByRole('region', { name: 'Nội dung' })).toHaveStyle({ overflowY: 'auto' })
    expect(screen.getByRole('region', { name: 'Câu hỏi', hidden: true })).toHaveStyle({ overflowY: 'auto' })
    expect(screen.getAllByText('Passage content')).toHaveLength(1)
    expect(screen.getAllByText('Question content')).toHaveLength(1)
  })

  test('shows accessible mobile tabs and keeps both pane contents mounted when switching', () => {
    render(<WorkspaceProbe />)
    const contentTab = screen.getByRole('tab', { name: 'Nội dung' })
    const questionTab = screen.getByRole('tab', { name: 'Câu hỏi' })
    expect(contentTab).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tabpanel', { name: 'Nội dung' })).toBeVisible()
    fireEvent.click(questionTab)
    expect(questionTab).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tabpanel', { name: 'Câu hỏi' })).toBeVisible()
    const passageRegion = screen.getByRole('region', { name: 'Nội dung', hidden: true })
    passageRegion.scrollTop = 240
    fireEvent.click(contentTab)
    expect(screen.getByRole('region', { name: 'Nội dung' })).toBe(passageRegion)
    expect(passageRegion.scrollTop).toBe(240)
    expect(screen.getByText('Passage content')).toBeInTheDocument()
    expect(screen.getByText('Question content')).toBeInTheDocument()
  })

  test('exposes a bounded separator and supports keyboard resize without pointer input', () => {
    const onChange = vi.fn()
    render(<WorkspaceDivider value={50} min={40} max={60} onChange={onChange} />)
    const separator = screen.getByRole('separator', { name: 'Điều chỉnh độ rộng hai khung' })
    expect(separator).toHaveAttribute('aria-valuemin', '40')
    expect(separator).toHaveAttribute('aria-valuemax', '60')
    expect(separator).toHaveAttribute('aria-valuenow', '50')
    fireEvent.keyDown(separator, { key: 'ArrowRight' })
    fireEvent.keyDown(separator, { key: 'Home' })
    fireEvent.keyDown(separator, { key: 'End' })
    expect(onChange.mock.calls.map(([value]) => value)).toEqual([60, 40, 60])
  })

  test('preset controls change ratios using real buttons', () => {
    const onChange = vi.fn()
    render(<WorkspacePresetControls value={40} onChange={onChange} />)
    fireEvent.click(screen.getByRole('button', { name: '60/40' }))
    expect(onChange).toHaveBeenCalledWith(60)
  })

  test('binds reading and writing ratios to the existing 1A preference API', () => {
    expect(WORKSPACE_RATIO_PRESETS).toEqual([40, 50, 60])
    expect(getWorkspacePreferenceKey('reading')).toBe('readingSplitRatio')
    expect(getWorkspacePreferenceKey('writing')).toBe('writingSplitRatio')
    expect(getWorkspaceRatio(DEFAULT_PREFERENCES, 'reading')).toBe(40)
    expect(isCompletePreferences(DEFAULT_PREFERENCES)).toBe(true)
    expect(normalizePreferences({ readingSplitRatio: 55 }).readingSplitRatio).toBe(40)
    const updatePreference = vi.fn()
    saveWorkspaceRatio(updatePreference, 'writing', 60)
    expect(updatePreference).toHaveBeenCalledWith('writingSplitRatio', 60)
  })

  test('exports the mobile tab primitive as a standalone interface', () => {
    render(<MobileWorkspaceTabs leftLabel="Nội dung" rightLabel="Bài viết" left={<p>Prompt</p>} right={<p>Editor</p>} />)
    expect(screen.getByRole('tab', { name: 'Bài viết' })).toBeInTheDocument()
  })
})
