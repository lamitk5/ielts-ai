import AnimatedSection from '../components/common/AnimatedSection'
import Button from '../components/common/Button'
import GlassCard from '../components/common/GlassCard'

function HomePage() {
  return (
    <main className="page-main">
      <AnimatedSection className="foundation-proof" aria-labelledby="home-title">
        <p className="eyebrow">Academic Luxury · Frontend Foundation</p>
        <h1 id="home-title" className="font-display">
          IELTS AI
        </h1>
        <p className="foundation-copy">
          A calm foundation for focused IELTS practice.
        </p>
        <div className="foundation-actions" aria-label="Button variants">
          <Button variant="primary">Primary action</Button>
          <Button variant="secondary">Secondary action</Button>
          <Button variant="ghost">Ghost action</Button>
        </div>
        <GlassCard className="proof-card">
          <span className="proof-label">Foundation ready</span>
          <p>Shared surfaces, restrained motion, and accessible navigation are in place.</p>
        </GlassCard>
      </AnimatedSection>
    </main>
  )
}

export default HomePage
