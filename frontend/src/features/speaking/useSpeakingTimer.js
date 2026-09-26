import { useState, useEffect, useRef, useCallback } from 'react'

export function useSpeakingTimer({
  initialDuration = 60,
  mode = 'PREPARATION',
  onComplete,
}) {
  const [secondsLeft, setSecondsLeft] = useState(Math.max(0, initialDuration))
  const [isRunning, setIsRunning] = useState(false)
  const timerRef = useRef(null)
  const onCompleteRef = useRef(onComplete)

  onCompleteRef.current = onComplete

  const clearTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const start = useCallback(() => {
    setIsRunning(true)
  }, [])

  const pause = useCallback(() => {
    setIsRunning(false)
    clearTimer()
  }, [clearTimer])

  const reset = useCallback((newDuration) => {
    setIsRunning(false)
    clearTimer()
    const validDuration = typeof newDuration === 'number' ? newDuration : initialDuration
    setSecondsLeft(Math.max(0, validDuration))
  }, [clearTimer, initialDuration])

  useEffect(() => {
    if (!isRunning) {
      clearTimer()
      return undefined
    }

    timerRef.current = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current)
          timerRef.current = null
          setIsRunning(false)
          try {
            onCompleteRef.current?.()
          } catch {}
          return 0
        }
        return prev - 1
      })
    }, 1000)

    return () => {
      clearTimer()
    }
  }, [isRunning, clearTimer])

  return {
    secondsLeft: Math.max(0, secondsLeft),
    isRunning,
    start,
    pause,
    reset,
  }
}

export default useSpeakingTimer
