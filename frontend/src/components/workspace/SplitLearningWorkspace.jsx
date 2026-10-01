import { usePreferences } from '../../features/preferences/PreferenceProvider'
import { getWorkspaceRatio, saveWorkspaceRatio } from '../../features/workspace/workspacePreferences'
import { MobileWorkspaceTabs } from './MobileWorkspaceTabs'
import { WorkspaceDivider } from './WorkspaceDivider'
import { WorkspacePresetControls } from './WorkspacePresetControls'
import './workspace.css'

export function SplitLearningWorkspace({ left, right, leftLabel, rightLabel, ratio, onRatioChange, mobileMode = false, workspace = 'reading' }) {
  const { preferences, updatePreference } = usePreferences()
  const currentRatio = ratio ?? getWorkspaceRatio(preferences, workspace)
  const changeRatio = (nextRatio) => {
    onRatioChange?.(nextRatio)
    saveWorkspaceRatio(updatePreference, workspace, nextRatio)
  }

  return (
    <div
      className={`split-learning-workspace${workspace === 'reading' ? ' reading-workspace-editorial' : ''}${workspace === 'writing' ? ' writing-workspace-editorial' : ''}${mobileMode ? ' split-learning-workspace-mobile' : ''}`}
      style={{ '--workspace-left-ratio': `${currentRatio}fr`, '--workspace-right-ratio': `${100 - currentRatio}fr` }}
    >
      <div className="workspace-desktop-controls">
        <WorkspacePresetControls value={currentRatio} onChange={changeRatio} />
      </div>
      <MobileWorkspaceTabs
        leftLabel={leftLabel}
        rightLabel={rightLabel}
        left={left}
        right={right}
        integrated
        forceMobile={mobileMode}
        divider={<WorkspaceDivider value={currentRatio} min={40} max={60} onChange={changeRatio} />}
      />
    </div>
  )
}

export default SplitLearningWorkspace
