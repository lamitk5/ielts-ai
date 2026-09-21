import { ArrowRight, BarChart3, BookOpen, Sparkles } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import AnimatedSection from '../common/AnimatedSection'
import Button from '../common/Button'
import HeroSearch from './HeroSearch'
import HeroVisual from './HeroVisual'

function HeroSection({ homepageState }) {
  const navigate = useNavigate()
  const demoLabel = homepageState?.user
    ? `Member demo · ${homepageState.user.firstName}`
    : 'Guest mode'

  return (
    <AnimatedSection className="hero-section" aria-labelledby="hero-title">
      <div className="hero-ambient" aria-hidden="true">
        <span className="hero-ambient-grid" />
        <span className="hero-ambient-vignette" />
      </div>
      <div className="hero-grid">
        <div className="hero-content">
          <p className="eyebrow hero-eyebrow">IELTS 4 KỸ NĂNG • AI TUTOR 24/7</p>
          <h1 id="hero-title" className="font-display hero-title">
            Bứt phá Band điểm IELTS cùng Trợ giảng AI Độc quyền
          </h1>
          <p className="hero-description">
            Luyện tập Reading, Listening, Writing và Speaking trên một nền tảng duy nhất.
            Nhận phản hồi theo ngữ cảnh và cải thiện từng kỹ năng cùng trợ giảng AI.
          </p>
          <p className="hero-demo-label" data-testid="demo-mode">
            {demoLabel}
          </p>
          <HeroSearch />
          <div className="hero-actions" aria-label="Assessment actions">
            <Button size="lg" onClick={() => navigate('/assessment')}>
              Làm bài Test đánh giá năng lực
            </Button>
            <a className="hero-secondary-action" href="#skills">
              Khám phá 4 kỹ năng
              <ArrowRight aria-hidden="true" size={16} />
            </a>
          </div>
          <div className="hero-trust-row" aria-label="Learning support indicators">
            <span>
              <BookOpen aria-hidden="true" size={15} />
              4 kỹ năng IELTS
            </span>
            <span>
              <Sparkles aria-hidden="true" size={15} />
              Phản hồi theo ngữ cảnh
            </span>
            <span>
              <BarChart3 aria-hidden="true" size={15} />
              Theo dõi tiến độ
            </span>
          </div>
        </div>
        <HeroVisual />
      </div>
    </AnimatedSection>
  )
}

export default HeroSection
