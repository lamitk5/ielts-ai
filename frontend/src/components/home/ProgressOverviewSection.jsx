import AnimatedSection from '../common/AnimatedSection'
import GlassCard from '../common/GlassCard'
import SectionTitle from '../common/SectionTitle'
import SkeletonBlock from '../common/SkeletonBlock'
import BandRadarChart from '../charts/BandRadarChart'
import CommonMistakesWidget from './CommonMistakesWidget'
import ExamCountdownCard from './ExamCountdownCard'
import TodaysFocusCard from '../learning/TodaysFocusCard'
import RoadmapWidget from '../learning/RoadmapWidget'
import StreakCard from '../learning/StreakCard'
import SkillEnergyGrid from '../learning/SkillEnergyGrid'
import { calculateMeaningfulStreak } from '../../features/learning/streakRules'
import { Link } from 'react-router-dom'

function ProgressOverviewSection({ isAuthenticated, state, loading = false }) {
  const hasMeasuredProgress = Array.isArray(state?.progress)
    && state.progress.some(({ band }) => Number.isFinite(band))
  const showMemberProgress = isAuthenticated && hasMeasuredProgress
  const streakInfo = calculateMeaningfulStreak(state?.activity || [])

  return (
    <AnimatedSection
      id="progress"
      className="progress-section"
      aria-label="Tiến độ luyện tập của bạn"
    >
      <div className="progress-inner">
        <SectionTitle
          eyebrow="THEO DÕI TIẾN BỘ"
          title="Tiến độ luyện tập của bạn"
          description={
            showMemberProgress
              ? 'Một góc nhìn bình tĩnh về bốn kỹ năng để bạn biết nên tập trung vào đâu tiếp theo.'
              : 'Bắt đầu bằng một bài đánh giá để mở bảng theo dõi cá nhân và luyện tập có định hướng.'
          }
        />

        {loading ? (
          <GlassCard className="progress-loading-card">
            <SkeletonBlock label="Đang tải tiến độ luyện tập" />
          </GlassCard>
        ) : showMemberProgress ? (
          <div className="progress-member-layout">
            <TodaysFocusCard roadmap={state.roadmap} />
            <div className="progress-layout">
              <div className="progress-main-column">
                <GlassCard className="progress-radar-card">
                  <div className="progress-card-heading">
                    <div>
                      <p className="progress-card-kicker">TỔNG QUAN 4 KỸ NĂNG</p>
                      <h3 className="font-display">Band theo từng kỹ năng</h3>
                    </div>
                    <span className="progress-estimate">Band ước lượng</span>
                  </div>
                  <BandRadarChart data={state.progress} />
                  <ul className="progress-band-list">
                    {state.progress.map(({ skill, band }) => (
                      <li key={skill}>
                        <span>{skill}</span>
                        <strong>{Number.isFinite(band) ? band : 'Chưa có dữ liệu'}</strong>
                      </li>
                    ))}
                  </ul>
                </GlassCard>
                <SkillEnergyGrid skills={state.skills || state.progress} />
              </div>
              <div className="progress-side-column">
                <StreakCard streakInfo={streakInfo} />
                <RoadmapWidget roadmap={state.roadmap} />
                <ExamCountdownCard examDate={state.user?.examDate} />
                <CommonMistakesWidget mistakes={state.mistakes} />
              </div>
            </div>
          </div>
        ) : (
          <GlassCard className="progress-guest-card">
            <div className="progress-guest-copy">
              <p className="progress-card-kicker">LỘ TRÌNH CÁ NHÂN</p>
              <h3 className="font-display">Đánh giá trình độ để mở bảng tiến độ cá nhân</h3>
              <p>
                Xác định điểm xuất phát cho Reading, Listening, Writing và Speaking — không phỏng đoán,
                chỉ từng bước rõ ràng hơn.
              </p>
            </div>
            <Link className="button btn-liquid button-primary button-md" to="/assessment">
              <span className="button-label">Bắt đầu đánh giá</span>
            </Link>
            <div className="progress-guest-preview progress-guest-preview-editorial">
              <div className="progress-guest-preview-heading">
                <p className="progress-card-kicker">XEM TRƯỚC LỘ TRÌNH</p>
                <span>4 kỹ năng</span>
              </div>
              <div className="progress-guest-preview-chart">
                <SkeletonBlock label="Xem trước tiến độ bốn kỹ năng" />
                <div role="img" aria-label="Xem trước tiến độ theo 4 kỹ năng">
                  <span className="progress-preview-ring progress-preview-ring-outer" />
                  <span className="progress-preview-ring progress-preview-ring-inner" />
                  <span className="progress-preview-axis progress-preview-axis-horizontal" />
                  <span className="progress-preview-axis progress-preview-axis-vertical" />
                  <span className="progress-preview-shape" />
                </div>
              </div>
              <ul className="progress-guest-skill-list" aria-label="Bốn kỹ năng trong lộ trình">
                {['Reading', 'Listening', 'Writing', 'Speaking'].map((skill) => (
                  <li key={skill}>
                    <span aria-hidden="true" />
                    {skill}
                  </li>
                ))}
              </ul>
            </div>
          </GlassCard>
        )}
      </div>
    </AnimatedSection>
  )
}

export default ProgressOverviewSection
