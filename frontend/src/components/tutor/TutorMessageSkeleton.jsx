import SkeletonBlock from '../common/SkeletonBlock'
import { GENERIC_PENDING_LABEL } from '../../features/tutor/tutorRequestState'

function TutorMessageSkeleton({ label = GENERIC_PENDING_LABEL }) {
  return <SkeletonBlock className="tutor-message-skeleton" label={label} />
}

export default TutorMessageSkeleton
