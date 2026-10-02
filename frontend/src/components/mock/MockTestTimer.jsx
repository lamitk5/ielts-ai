import React, { useEffect, useState } from 'react'

function formatTime(totalSeconds) {
  if (totalSeconds < 0) totalSeconds = 0
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  const pad = (n) => String(n).padStart(2, '0')
  if (hours > 0) {
    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
  }
  return `${pad(minutes)}:${pad(seconds)}`
}

export default function MockTestTimer({
  totalTimeLimitSeconds = 10200,
  initialElapsedSeconds = 0,
  isPaused = false,
  onExpire,
  className = '',
}) {
  const [elapsed, setElapsed] = useState(initialElapsedSeconds)

  useEffect(() => {
    setElapsed(initialElapsedSeconds)
  }, [initialElapsedSeconds])

  useEffect(() => {
    if (isPaused) return

    const timer = setInterval(() => {
      setElapsed((prev) => {
        const next = prev + 1
        if (next >= totalTimeLimitSeconds) {
          clearInterval(timer)
          if (onExpire) onExpire()
          return totalTimeLimitSeconds
        }
        return next
      })
    }, 1000)

    return () => clearInterval(timer)
  }, [isPaused, totalTimeLimitSeconds, onExpire])

  const remaining = Math.max(0, totalTimeLimitSeconds - elapsed)
  const isUrgent = remaining <= 300 && remaining > 0
  const isCritical = remaining <= 60 && remaining > 0

  let urgencyClass = 'text-navy-900 dark:text-gold-200'
  if (isCritical) {
    urgencyClass = 'text-red-600 dark:text-red-400 animate-pulse font-bold'
  } else if (isUrgent) {
    urgencyClass = 'text-amber-600 dark:text-amber-400 font-semibold'
  }

  return (
    <div
      className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-gold-500/20 bg-gold-500/5 ${urgencyClass} ${className}`.trim()}
      role="timer"
      aria-label={`Thời gian làm bài còn lại: ${formatTime(remaining)}`}
      aria-live="polite"
    >
      <svg
        className="w-4 h-4 opacity-80"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2"
          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
        />
      </svg>
      <span className="font-mono text-sm tracking-wide">
        {formatTime(remaining)}
      </span>
      {isPaused && (
        <span className="text-xs uppercase tracking-wider px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-600 dark:text-amber-300">
          Tạm dừng
        </span>
      )}
    </div>
  )
}
