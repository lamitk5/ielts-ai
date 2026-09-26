import { useId, useState } from 'react'

export function MobileWorkspaceTabs({ leftLabel, rightLabel, left, right, divider, integrated = false, activeTab: controlledTab, onTabChange }) {
  const [uncontrolledTab, setUncontrolledTab] = useState(0)
  const instanceId = useId()
  const activeTab = controlledTab ?? uncontrolledTab
  const selectTab = (index) => {
    if (controlledTab === undefined) setUncontrolledTab(index)
    onTabChange?.(index)
  }
  const tabs = [
    { label: leftLabel, content: left },
    { label: rightLabel, content: right },
  ]

  return (
    <div className={`workspace-mobile${integrated ? ' workspace-integrated' : ''}`}>
      <div className="workspace-mobile-tabs" role="tablist" aria-label="Khung học tập">
        {tabs.map((tab, index) => (
          <button
            key={tab.label}
            id={`${instanceId}-workspace-tab-${index}`}
            type="button"
            role="tab"
            aria-selected={activeTab === index}
            aria-controls={`${instanceId}-workspace-panel-${index}`}
            tabIndex={activeTab === index ? 0 : -1}
            onClick={() => selectTab(index)}
            onKeyDown={(event) => {
              if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
                event.preventDefault()
                const next = (index + (event.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length
                selectTab(next)
                document.getElementById(`${instanceId}-workspace-tab-${next}`)?.focus()
              }
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div className="workspace-panes">
        <section id={`${instanceId}-workspace-panel-0`} className="workspace-mobile-panel" role="tabpanel" aria-labelledby={`${instanceId}-workspace-tab-0`} hidden={activeTab !== 0} tabIndex={0}>
          <div className="workspace-pane" role="region" aria-label={leftLabel} tabIndex={0} style={{ overflowY: 'auto', minWidth: 0 }}>{left}</div>
        </section>
        {divider}
        <section id={`${instanceId}-workspace-panel-1`} className="workspace-mobile-panel" role="tabpanel" aria-labelledby={`${instanceId}-workspace-tab-1`} hidden={activeTab !== 1} tabIndex={0}>
          <div className="workspace-pane" role="region" aria-label={rightLabel} tabIndex={0} style={{ overflowY: 'auto', minWidth: 0 }}>{right}</div>
        </section>
      </div>
    </div>
  )
}
