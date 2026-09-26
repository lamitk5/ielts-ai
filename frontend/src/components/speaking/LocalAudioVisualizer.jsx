export function LocalAudioVisualizer({
  amplitude = 0,
  isRecording = false,
  reducedMotion = false,
}) {
  const barCount = 7
  // Generate relative heights based on amplitude
  const bars = Array.from({ length: barCount }, (_, i) => {
    if (!isRecording) return 15
    const distanceToCenter = Math.abs(i - Math.floor(barCount / 2))
    const factor = Math.max(0.2, 1 - distanceToCenter * 0.2)
    const heightPercent = Math.min(100, Math.max(15, amplitude * 100 * factor + 15))
    return Math.round(heightPercent)
  })

  return (
    <div
      className={`local-visualizer ${reducedMotion ? 'reduced-motion' : ''}`}
      role="img"
      aria-label="Biểu đồ sóng âm microphone cục bộ"
    >
      <div className="visualizer-bars">
        {bars.map((height, idx) => (
          <div
            key={idx}
            className="visualizer-bar"
            style={{
              height: reducedMotion ? '40%' : `${height}%`,
              opacity: isRecording ? 0.9 : 0.4,
            }}
          />
        ))}
      </div>
      <p className="visualizer-note">
        Biểu đồ sóng âm cục bộ (không lưu trữ hay gửi âm thanh lên máy chủ)
      </p>
    </div>
  )
}

export default LocalAudioVisualizer
