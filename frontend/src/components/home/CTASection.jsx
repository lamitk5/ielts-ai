import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import AnimatedSection from '../common/AnimatedSection'

function CTASection() {
  return (
    <AnimatedSection className="final-cta-section" aria-labelledby="final-cta-title">
      <div className="final-cta-inner">
        <p className="eyebrow">BƯỚC TIẾP THEO</p>
        <h2 id="final-cta-title" className="font-display">Sẵn sàng bắt đầu lộ trình IELTS của bạn?</h2>
        <p>Đánh giá điểm xuất phát và chọn cách luyện phù hợp cho cả bốn kỹ năng.</p>
        <div className="final-cta-actions">
          <Link className="button btn-liquid button-primary button-lg" to="/assessment">
            <span className="button-label">Bắt đầu đánh giá năng lực</span>
          </Link>
          <Link className="final-cta-secondary" to="#skills">
            Luyện tập 4 kỹ năng
            <ArrowRight aria-hidden="true" size={16} />
          </Link>
        </div>
      </div>
    </AnimatedSection>
  )
}

export default CTASection
