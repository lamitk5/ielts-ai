import { BrainCircuit, Circle, Sparkles } from 'lucide-react'
import GlassCard from '../common/GlassCard'
import ParallaxLayer from '../motion/ParallaxLayer'

const skillSignals = ['R', 'L', 'W', 'S']

function HeroVisual() {
  return (
    <div className="hero-visual" data-testid="hero-visual" aria-hidden="true">
      <div className="hero-visual-glow hero-visual-glow-gold" />
      <div className="hero-visual-glow hero-visual-glow-blue" />
      <ParallaxLayer className="hero-orbit hero-orbit-back" distance={12}>
        <span className="hero-orbit-ring hero-orbit-ring-large" />
        <span className="hero-orbit-ring hero-orbit-ring-small" />
      </ParallaxLayer>
      <ParallaxLayer className="hero-orbit hero-orbit-front" distance={8}>
        <span className="hero-orbit-line hero-orbit-line-one" />
        <span className="hero-orbit-line hero-orbit-line-two" />
        <span className="hero-orbit-dot hero-orbit-dot-one" />
        <span className="hero-orbit-dot hero-orbit-dot-two" />
      </ParallaxLayer>
      <GlassCard className="hero-intelligence-card">
        <div className="hero-intelligence-header">
          <span className="hero-intelligence-kicker">Learning intelligence</span>
          <Sparkles aria-hidden="true" size={16} />
        </div>
        <div className="hero-intelligence-core">
          <span className="hero-core-halo" />
          <span className="hero-core-orb">
            <BrainCircuit aria-hidden="true" size={30} />
          </span>
        </div>
        <div className="hero-signal-grid">
          {skillSignals.map((signal) => (
            <div className="hero-signal" key={signal}>
              <Circle aria-hidden="true" size={11} />
              <span>{signal}</span>
            </div>
          ))}
        </div>
        <div className="hero-intelligence-footer">
          <span>IELTS AI Tutor</span>
          <span className="hero-intelligence-status">Context ready</span>
        </div>
      </GlassCard>
    </div>
  )
}

export default HeroVisual
