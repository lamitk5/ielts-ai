import AnimatedSection from '../common/AnimatedSection'
import GlassCard from '../common/GlassCard'
import SectionTitle from '../common/SectionTitle'

const previewPrompts = [
  'Giải thích lỗi Writing của tôi',
  'Vì sao đáp án Reading này sai?',
  'Luyện Speaking Part 2',
]

function TutorPreviewSection() {
  return (
    <AnimatedSection id="ai-tutor" className="tutor-preview-section" aria-label="Én">
      <div className="tutor-preview-inner">
        <SectionTitle
          eyebrow="TRỢ GIẢNG THEO NGỮ CẢNH"
          title="Học sâu hơn với phản hồi đúng lúc"
          description="Én giúp bạn hiểu vì sao một đáp án đúng, nhìn rõ lỗi trong bài viết và chuẩn bị tự tin hơn cho phần Speaking."
        />
        <GlassCard className="tutor-preview-card">
          <div className="tutor-preview-orb" aria-hidden="true" />
          <div className="tutor-preview-copy">
            <p className="progress-card-kicker">SẴN SÀNG KHI BẠN CẦN</p>
            <h3 className="font-display">Một người bạn học luôn nhớ ngữ cảnh bài luyện</h3>
            <p>Hỏi ngắn, nhận gợi ý rõ ràng và tiếp tục luyện tập mà không rời khỏi mạch học.</p>
          </div>
          <div className="tutor-preview-prompts" aria-label="Ví dụ câu hỏi cho Én">
            {previewPrompts.map((prompt) => <span key={prompt}>{prompt}</span>)}
          </div>
        </GlassCard>
      </div>
    </AnimatedSection>
  )
}

export default TutorPreviewSection
