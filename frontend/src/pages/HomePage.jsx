import { useSearchParams } from 'react-router-dom'
import HeroSection from '../components/home/HeroSection'
import { guestDemo, memberDemo } from '../data/homepageMockData'

function HomePage() {
  const [searchParams] = useSearchParams()
  const isMemberDemo = searchParams.get('demo') === 'member'
  const homepageState = isMemberDemo ? memberDemo : guestDemo

  return <HeroSection homepageState={homepageState} />
}

export default HomePage
