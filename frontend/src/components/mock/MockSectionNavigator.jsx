import React from 'react'

const SKILL_LABELS = {
  LISTENING: 'Listening (Nghe)',
  READING: 'Reading (Đọc)',
  WRITING: 'Writing (Viết)',
  SPEAKING: 'Speaking (Nói)',
}

export default function MockSectionNavigator({
  sections = [],
  currentSectionIndex = 0,
  onSelectSection,
  disabled = false,
}) {
  return (
    <nav aria-label="Mock Test Sections" className="flex items-center gap-2 overflow-x-auto py-1">
      {sections.map((section, idx) => {
        const isCurrent = idx === currentSectionIndex
        const isCompleted = section.status === 'COMPLETED'
        const skillKey = (section.skill || '').toUpperCase()
        const label = SKILL_LABELS[skillKey] || section.skill

        let badgeClass = 'border-slate-300 dark:border-slate-700 text-slate-500 hover:border-gold-500/50'
        if (isCurrent) {
          badgeClass = 'border-gold-500 bg-gold-500/10 text-gold-600 dark:text-gold-300 font-semibold shadow-sm'
        } else if (isCompleted) {
          badgeClass = 'border-emerald-500/40 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
        }

        return (
          <button
            key={section.id || idx}
            type="button"
            disabled={disabled}
            onClick={() => onSelectSection && onSelectSection(idx)}
            aria-current={isCurrent ? 'step' : undefined}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs sm:text-sm whitespace-nowrap transition-all focus:outline-none focus:ring-2 focus:ring-gold-500 ${badgeClass}`}
          >
            <span className="flex items-center justify-center w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-[11px] font-mono">
              {idx + 1}
            </span>
            <span>{label}</span>
            {isCompleted && (
              <svg
                className="w-3.5 h-3.5 text-emerald-500 ml-0.5"
                fill="currentColor"
                viewBox="0 0 20 20"
                aria-hidden="true"
              >
                <path
                  fillRule="evenodd"
                  d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                  clipRule="evenodd"
                />
              </svg>
            )}
          </button>
        )
      })}
    </nav>
  )
}
