import { useNavigate, useSearchParams } from 'react-router-dom'
import AnimatedSection from '../components/common/AnimatedSection'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'
import { guestDemo, memberDemo } from '../data/homepageMockData'

function HomePage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const isMemberDemo = searchParams.get('demo') === 'member'
  const homepageState = isMemberDemo ? memberDemo : guestDemo

  return (
    <AnimatedSection
      className="foundation-proof home-shell"
      aria-labelledby="home-title"
    >
      <p className="eyebrow">Academic Luxury · App Shell</p>
      <h1 id="home-title" className="font-display">
        IELTS AI
      </h1>
      <p className="foundation-copy">
        A calm foundation for focused IELTS practice across the four core skills.
      </p>
      <p data-testid="demo-mode" className="proof-label">
        {isMemberDemo ? `Member demo · ${homepageState.user.firstName}` : 'Guest mode'}
      </p>
      <div className="foundation-actions" aria-label="Assessment entry">
        <Button variant="primary" onClick={() => navigate('/assessment')}>
          Bắt đầu đánh giá
        </Button>
        <Button variant="secondary">Secondary action</Button>
        <Button variant="ghost">Ghost action</Button>
      </div>
      <GlassCard className="proof-card">
        <span className="proof-label">Foundation shell ready</span>
        <p>Future four-skill homepage regions will compose here without adding feature logic.</p>
      </GlassCard>
    </AnimatedSection>
  )
}

export default HomePage
