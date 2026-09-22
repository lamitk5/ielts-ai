import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import HeroSection from '../components/home/HeroSection'
import CTASection from '../components/home/CTASection'
import ProgressOverviewSection from '../components/home/ProgressOverviewSection'
import SkillsSection from '../components/home/SkillsSection'
import TutorPreviewSection from '../components/home/TutorPreviewSection'
import FloatingTutor from '../components/tutor/FloatingTutor'
import { guestDemo, memberDemo, skillCards } from '../data/homepageMockData'
import { useAuth } from '../features/auth/AuthProvider'
import { getMemberProgress } from '../services/progressApi'

function HomePage() {
  const [searchParams] = useSearchParams()
  const { isAuthenticated } = useAuth()
  const isMemberDemo = searchParams.get('demo') === 'member'
  const [memberState, setMemberState] = useState(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    if (!isAuthenticated || isMemberDemo) return undefined
    let active = true
    setIsLoading(true)
    getMemberProgress()
      .then((nextState) => active && setMemberState(nextState))
      .catch(() => active && setMemberState(null))
      .finally(() => active && setIsLoading(false))
    return () => { active = false }
  }, [isAuthenticated, isMemberDemo])

  const homepageState = isMemberDemo ? memberDemo : memberState ?? guestDemo
  const showMemberProgress = isMemberDemo || Boolean(memberState?.progress)

  return (
    <>
      <HeroSection homepageState={homepageState} />
      <SkillsSection skills={skillCards} />
      <ProgressOverviewSection isAuthenticated={showMemberProgress} state={homepageState} loading={isAuthenticated && !isMemberDemo && isLoading} />
      <TutorPreviewSection />
      <CTASection />
      <FloatingTutor />
    </>
  )
}

export default HomePage
