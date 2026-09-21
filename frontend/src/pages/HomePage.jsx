import { useSearchParams } from 'react-router-dom'
import HeroSection from '../components/home/HeroSection'
import ProgressOverviewSection from '../components/home/ProgressOverviewSection'
import SkillsSection from '../components/home/SkillsSection'
import { guestDemo, memberDemo, skillCards } from '../data/homepageMockData'

function HomePage() {
  const [searchParams] = useSearchParams()
  const isMemberDemo = searchParams.get('demo') === 'member'
  const homepageState = isMemberDemo ? memberDemo : guestDemo

  return (
    <>
      <HeroSection homepageState={homepageState} />
      <SkillsSection skills={skillCards} />
      <ProgressOverviewSection isAuthenticated={isMemberDemo} state={homepageState} />
    </>
  )
}

export default HomePage
