import { useSearchParams } from 'react-router-dom'
import HeroSection from '../components/home/HeroSection'
import CTASection from '../components/home/CTASection'
import ProgressOverviewSection from '../components/home/ProgressOverviewSection'
import SkillsSection from '../components/home/SkillsSection'
import TutorPreviewSection from '../components/home/TutorPreviewSection'
import FloatingTutor from '../components/tutor/FloatingTutor'
import { guestDemo, memberDemo, skillCards } from '../data/homepageMockData'
import { useAuth } from '../features/auth/AuthProvider'
import { OnboardingEntry } from '../components/onboarding/OnboardingEntry'
import { LEARNING_STATE_STATUS, useLearningIntelligence } from '../features/learning/learningIntelligenceState'

function HomePage() {
  const [searchParams] = useSearchParams()
  const { isAuthenticated, session } = useAuth()
  const isMemberDemo = searchParams.get('demo') === 'member'
  const learning = useLearningIntelligence({ enabled: isAuthenticated && !isMemberDemo })
  const intelligenceState = learning.status === LEARNING_STATE_STATUS.READY
    || learning.status === LEARNING_STATE_STATUS.EMPTY
    ? {
        user: {
          firstName: session?.user?.firstName ?? null,
          targetBand: learning.profile.targetBand,
          examDate: null,
        },
        progress: learning.skills.map(({ name, band }) => ({ skill: name, band })),
        skills: learning.skills,
        mistakes: learning.mistakes,
        roadmap: learning.roadmap,
        activity: learning.activity,
      }
    : null

  const homepageState = isMemberDemo ? memberDemo : intelligenceState ?? guestDemo
  const showMemberProgress = isMemberDemo || Boolean(intelligenceState?.progress)

  return (
    <>
      <HeroSection homepageState={homepageState} />
      <SkillsSection skills={skillCards} />
      <ProgressOverviewSection
        isAuthenticated={showMemberProgress}
        state={homepageState}
        loading={isAuthenticated && !isMemberDemo && learning.status === LEARNING_STATE_STATUS.LOADING}
        enableTodaysPlan={isAuthenticated && !isMemberDemo}
      />
      <TutorPreviewSection />
      <OnboardingEntry />
      <CTASection />
      <FloatingTutor />
    </>
  )
}

export default HomePage
