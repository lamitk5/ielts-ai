import { ArrowRight, BarChart3, BookOpen, Sparkles } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import AnimatedSection from '../common/AnimatedSection'
import Button from '../common/Button'
import HeroSearch, { HeroSuggestions } from './HeroSearch'
import HeroVisual from './HeroVisual'

function HeroSection() {
  const navigate = useNavigate()

  return (
    <AnimatedSection className="hero-section hero-editorial" aria-labelledby="hero-title">
      <div className="hero-ambient" aria-hidden="true">
        <span className="hero-ambient-grid" />
        <span className="hero-ambient-vignette" />
      </div>
      <div className="hero-grid">
        <div className="hero-content">
          <p className="eyebrow hero-eyebrow">IELTS 4 KỸ NĂNG • AI TUTOR 24/7</p>
          <h1 id="hero-title" className="font-display hero-title">
            <span className="hero-title-line">Bứt phá Band điểm IELTS</span>{' '}
            <span className="hero-title-line">cùng <span className="hero-title-phrase">Trợ giảng AI</span></span>{' '}
            <span className="hero-title-line">Độc quyền</span>
          </h1>
          <p className="hero-description">
            Luyện tập Reading, Listening, Writing và Speaking trên một nền tảng duy nhất.
            Nhận phản hồi theo ngữ cảnh và cải thiện từng kỹ năng cùng trợ giảng AI.
          </p>
          <HeroSearch />
          <div className="hero-actions" aria-label="Assessment actions">
            <Button size="lg" onClick={() => navigate('/assessment')}>
              Làm bài Test đánh giá năng lực
            </Button>
            <Link className="hero-secondary-action" to="/#skills">
              Khám phá 4 kỹ năng
              <ArrowRight aria-hidden="true" size={16} />
            </Link>
          </div>
          <HeroSuggestions />
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
