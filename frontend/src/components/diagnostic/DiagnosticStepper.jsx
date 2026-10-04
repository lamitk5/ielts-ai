import GlassCard from '../common/GlassCard'

const skills = ['READING', 'LISTENING', 'WRITING', 'SPEAKING']

export default function DiagnosticStepper({ session, sections = [], onUnavailable, onFinish, busy = false }) {
  const sectionBySkill = Object.fromEntries(sections.map((section) => [section.skill, section]))
  return (
    <GlassCard className="diagnostic-stepper">
      <div className="diagnostic-stepper-heading">
        <div>
          <p className="eyebrow">ĐÁNH GIÁ KHỞI ĐIỂM</p>
          <h2 className="font-display">Bốn kỹ năng, một điểm xuất phát rõ ràng</h2>
        </div>
        <span className="status-chip">{session?.state === 'COMPLETED' ? 'Đã hoàn tất' : 'Đang thực hiện'}</span>
      </div>
      <p className="foundation-copy">Kết quả chỉ được ghi nhận khi có dữ liệu luyện tập thật. Phần chưa đủ dữ liệu sẽ được giữ là chưa đủ bằng chứng.</p>
      <div className="diagnostic-skill-grid">
        {skills.map((skill) => {
          const section = sectionBySkill[skill]
          const ready = section?.state === 'READY'
          return (
            <div className="diagnostic-skill-card" key={skill}>
              <span className="progress-card-kicker">{skill}</span>
              <strong>{ready ? `${section.score}/${section.total}` : 'Chưa có dữ liệu đủ tin cậy'}</strong>
              <span>{ready ? 'Đã ghi nhận từ bài nộp canonical' : 'Có thể đánh dấu chưa khả dụng'}</span>
              {!ready && session?.id ? <button type="button" className="button button-ghost button-sm" disabled={busy} onClick={() => onUnavailable?.(skill)}>Đánh dấu chưa khả dụng</button> : null}
            </div>
          )
        })}
      </div>
      {session?.id && session.state !== 'COMPLETED' ? <button type="button" className="button btn-liquid button-primary button-md" disabled={busy} onClick={() => onFinish?.()}>{busy ? 'Đang lưu…' : 'Hoàn tất đánh giá'}</button> : null}
    </GlassCard>
  )
}
