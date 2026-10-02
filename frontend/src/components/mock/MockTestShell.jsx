import React from 'react'
import Button from '../common/Button'
import MockTestTimer from './MockTestTimer'
import MockSectionNavigator from './MockSectionNavigator'

export default function MockTestShell({
  session,
  currentSectionIndex = 0,
  onSelectSection,
  onPause,
  onResume,
  onNextSection,
  onSubmitTest,
  isSaving = false,
  saveError = null,
  children,
}) {
  const isPaused = session?.status === 'PAUSED'
  const isCompleted = session?.status === 'COMPLETED' || session?.status === 'EXPIRED'
  const currentSection = session?.sections?.[currentSectionIndex]
  const isLastSection = session?.sections ? currentSectionIndex >= session.sections.length - 1 : false

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-navy-950 text-slate-900 dark:text-slate-100 flex flex-col">
      {/* Top Header Bar */}
      <header className="sticky top-0 z-30 bg-white/80 dark:bg-navy-900/80 backdrop-blur border-b border-slate-200 dark:border-slate-800 px-4 py-3 shadow-sm">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-serif text-lg md:text-xl font-bold text-navy-900 dark:text-gold-100">
                  IELTS Academic Mock Test
                </h1>
                <span className="inline-block px-2 py-0.5 rounded text-[11px] font-medium bg-amber-500/10 text-amber-700 dark:text-amber-300 border border-amber-500/20">
                  Thi thử mô phỏng AI
                </span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Phiên làm bài: <span className="font-mono">{session?.id?.slice(0, 8)}...</span> • Kết quả ước lượng bởi AI
              </p>
            </div>
          </div>

          {/* Center / Section Navigator */}
          {session?.sections && (
            <MockSectionNavigator
              sections={session.sections}
              currentSectionIndex={currentSectionIndex}
              onSelectSection={onSelectSection}
              disabled={isPaused || isCompleted}
            />
          )}

          {/* Right Controls: Timer, Autosave, Action */}
          <div className="flex items-center gap-3 self-end md:self-auto">
            {/* Autosave status indicator */}
            <div className="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1.5" aria-live="polite">
              {isSaving ? (
                <span className="inline-flex items-center gap-1 text-amber-600 dark:text-amber-400">
                  <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-ping" />
                  Đang lưu...
                </span>
              ) : saveError ? (
                <span className="text-red-500" title={saveError}>
                  Lỗi lưu nháp
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                  Đã tự động lưu
                </span>
              )}
            </div>

            {/* Timer */}
            {session && (
              <MockTestTimer
                totalTimeLimitSeconds={session.totalTimeLimitSeconds}
                initialElapsedSeconds={session.elapsedSeconds}
                isPaused={isPaused || isCompleted}
              />
            )}

            {/* Pause / Resume Button */}
            {!isCompleted && (
              isPaused ? (
                <Button size="sm" variant="secondary" onClick={onResume}>
                  Tiếp tục
                </Button>
              ) : (
                <Button size="sm" variant="ghost" onClick={onPause} aria-label="Tạm dừng làm bài">
                  Tạm dừng
                </Button>
              )
            )}

            {/* Next or Complete */}
            {!isCompleted && (
              isLastSection ? (
                <Button size="sm" variant="primary" onClick={onSubmitTest}>
                  Nộp bài thi thử
                </Button>
              ) : (
                <Button size="sm" variant="primary" onClick={onNextSection}>
                  Phần tiếp theo →
                </Button>
              )
            )}
          </div>
        </div>
      </header>

      {/* Main Workspace Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-6 relative">
        {children}

        {/* Pause Overlay Modal */}
        {isPaused && (
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="pause-dialog-title"
            className="fixed inset-0 z-50 bg-navy-950/70 backdrop-blur-sm flex items-center justify-center p-4"
          >
            <div className="bg-white dark:bg-navy-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 md:p-8 max-w-md w-full text-center shadow-2xl space-y-4">
              <div className="w-12 h-12 mx-auto rounded-full bg-amber-500/10 text-amber-500 flex items-center justify-center">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h2 id="pause-dialog-title" className="font-serif text-xl font-bold text-navy-900 dark:text-gold-100">
                Bài thi thử đang tạm dừng
              </h2>
              <p className="text-sm text-slate-600 dark:text-slate-300">
                Đồng hồ đã tạm dừng và câu trả lời của bạn được lưu an toàn. Bạn có thể tiếp tục bất cứ lúc nào.
              </p>
              <div className="pt-2">
                <Button variant="primary" className="w-full" onClick={onResume}>
                  Tiếp tục làm bài
                </Button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
