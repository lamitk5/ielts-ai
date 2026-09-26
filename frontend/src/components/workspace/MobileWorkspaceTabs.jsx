import { useId, useLayoutEffect, useRef, useState, useSyncExternalStore } from 'react'

const MOBILE_QUERY = '(max-width: 1023px)'

function subscribeToViewport(onChange) {
  const query = window.matchMedia?.(MOBILE_QUERY)
  if (!query) {
    window.addEventListener('resize', onChange)
    return () => window.removeEventListener('resize', onChange)
  }
  if (query.addEventListener) query.addEventListener('change', onChange)
  else query.addListener?.(onChange)
  return () => {
    if (query.removeEventListener) query.removeEventListener('change', onChange)
    else query.removeListener?.(onChange)
  }
}

function isMobileViewport() {
  return window.matchMedia?.(MOBILE_QUERY).matches ?? window.innerWidth <= 1023
}

export function MobileWorkspaceTabs({ leftLabel, rightLabel, left, right, divider, integrated = false, forceMobile = false, activeTab: controlledTab, onTabChange }) {
  const [uncontrolledTab, setUncontrolledTab] = useState(0)
  const instanceId = useId()
  const paneRefs = useRef([null, null])
  const scrollPositions = useRef([0, 0])
  const mobileViewport = useSyncExternalStore(subscribeToViewport, isMobileViewport, () => false)
  const tabMode = !integrated || forceMobile || mobileViewport
  const activeTab = controlledTab ?? uncontrolledTab
  useLayoutEffect(() => {
    if (!tabMode) return
    const activePane = paneRefs.current[activeTab]
    if (activePane) activePane.scrollTop = scrollPositions.current[activeTab]
  }, [activeTab, tabMode])

  const rememberScroll = (index, event) => {
    if (index === activeTab) scrollPositions.current[index] = event.currentTarget.scrollTop
  }

  const selectTab = (index) => {
    const outgoingPane = paneRefs.current[activeTab]
    if (outgoingPane) scrollPositions.current[activeTab] = outgoingPane.scrollTop
    if (controlledTab === undefined) setUncontrolledTab(index)
    onTabChange?.(index)
  }
  const tabs = [
    { label: leftLabel, content: left },
    { label: rightLabel, content: right },
  ]

  return (
    <div className={`workspace-mobile${integrated ? ' workspace-integrated' : ''}${tabMode ? ' workspace-tab-mode' : ''}`}>
      {tabMode && <div className="workspace-mobile-tabs" role="tablist" aria-label="Khung học tập">
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
      </div>}
      <div className="workspace-panes">
        <section
          id={`${instanceId}-workspace-panel-0`}
          className="workspace-mobile-panel"
          role={tabMode ? 'tabpanel' : 'region'}
          aria-label={tabMode ? undefined : leftLabel}
          aria-labelledby={tabMode ? `${instanceId}-workspace-tab-0` : undefined}
          hidden={tabMode ? activeTab !== 0 : undefined}
          tabIndex={tabMode ? 0 : undefined}
        >
          <div ref={(node) => { paneRefs.current[0] = node }} className="workspace-pane" role={tabMode ? 'region' : undefined} aria-label={tabMode ? leftLabel : undefined} tabIndex={0} onScroll={(event) => rememberScroll(0, event)}>{left}</div>
        </section>
        {divider}
        <section
          id={`${instanceId}-workspace-panel-1`}
          className="workspace-mobile-panel"
          role={tabMode ? 'tabpanel' : 'region'}
          aria-label={tabMode ? undefined : rightLabel}
          aria-labelledby={tabMode ? `${instanceId}-workspace-tab-1` : undefined}
          hidden={tabMode ? activeTab !== 1 : undefined}
          tabIndex={tabMode ? 0 : undefined}
        >
          <div ref={(node) => { paneRefs.current[1] = node }} className="workspace-pane" role={tabMode ? 'region' : undefined} aria-label={tabMode ? rightLabel : undefined} tabIndex={0} onScroll={(event) => rememberScroll(1, event)}>{right}</div>
        </section>
      </div>
    </div>
  )
}
