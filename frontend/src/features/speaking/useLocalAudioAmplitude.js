import { useState, useRef, useEffect, useCallback } from 'react'

export function useLocalAudioAmplitude({
  isRecording = false,
  onPermissionChange,
} = {}) {
  const [permissionState, setPermissionState] = useState('prompt')
  const [amplitude, setAmplitude] = useState(0)

  const streamRef = useRef(null)
  const audioContextRef = useRef(null)
  const analyserRef = useRef(null)
  const animFrameRef = useRef(null)

  const stopAudio = useCallback(() => {
    if (animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current)
      animFrameRef.current = null
    }

    if (streamRef.current) {
      const tracks = streamRef.current.getTracks?.() || []
      tracks.forEach((track) => track.stop?.())
      streamRef.current = null
    }

    if (audioContextRef.current) {
      try {
        audioContextRef.current.close?.()
      } catch {
        // ignore close error
      }
      audioContextRef.current = null
    }

    analyserRef.current = null
    setAmplitude(0)
  }, [])

  const loopRef = useRef()

  const updateAmplitudeLoop = useCallback(() => {
    if (!analyserRef.current) return

    const analyser = analyserRef.current
    const dataArray = new Uint8Array(analyser.frequencyBinCount)
    analyser.getByteFrequencyData(dataArray)

    let sum = 0
    for (let i = 0; i < dataArray.length; i++) {
      sum += dataArray[i]
    }
    const avg = dataArray.length > 0 ? sum / dataArray.length : 0
    const normalized = Math.min(1, Math.max(0, avg / 128))
    setAmplitude(normalized)

    animFrameRef.current = requestAnimationFrame(() => {
      loopRef.current?.()
    })
  }, [])

  useEffect(() => {
    loopRef.current = updateAmplitudeLoop
  }, [updateAmplitudeLoop])

  const requestPermission = useCallback(async () => {
    if (!navigator?.mediaDevices?.getUserMedia) {
      setPermissionState('unavailable')
      onPermissionChange?.('unavailable')
      return false
    }

    setPermissionState('requesting')

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream

      const AudioCtx = window.AudioContext || window.webkitAudioContext
      if (AudioCtx) {
        const audioCtx = new AudioCtx()
        audioContextRef.current = audioCtx
        const source = audioCtx.createMediaStreamSource(stream)
        const analyser = audioCtx.createAnalyser()
        analyser.fftSize = 256
        source.connect(analyser)
        analyserRef.current = analyser

        if (isRecording) {
          updateAmplitudeLoop()
        }
      }

      setPermissionState('granted')
      onPermissionChange?.('granted')
      return true
    } catch {
      setPermissionState('denied')
      onPermissionChange?.('denied')
      stopAudio()
      return false
    }
  }, [isRecording, onPermissionChange, stopAudio, updateAmplitudeLoop])

  useEffect(() => {
    if (isRecording && permissionState === 'granted' && analyserRef.current && !animFrameRef.current) {
      updateAmplitudeLoop()
    } else if (!isRecording && animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current)
      animFrameRef.current = null
      setAmplitude(0)
    }
  }, [isRecording, permissionState, updateAmplitudeLoop])

  useEffect(() => {
    return () => {
      stopAudio()
    }
  }, [stopAudio])

  return {
    permissionState,
    amplitude,
    requestPermission,
    stopAudio,
  }
}

export default useLocalAudioAmplitude
