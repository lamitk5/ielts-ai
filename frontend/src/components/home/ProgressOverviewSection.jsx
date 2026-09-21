import AnimatedSection from '../common/AnimatedSection'
import GlassCard from '../common/GlassCard'
import SectionTitle from '../common/SectionTitle'
import SkeletonBlock from '../common/SkeletonBlock'
import BandRadarChart from '../charts/BandRadarChart'
import CommonMistakesWidget from './CommonMistakesWidget'
import ExamCountdownCard from './ExamCountdownCard'
import { Link } from 'react-router-dom'

function ProgressOverviewSection({ isAuthenticated, state, loading = false }) {
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
            isAuthenticated
              ? 'Một góc nhìn bình tĩnh về bốn kỹ năng để bạn biết nên tập trung vào đâu tiếp theo.'
              : 'Bắt đầu bằng một bài đánh giá để mở bảng theo dõi cá nhân và luyện tập có định hướng.'
          }
        />

        {loading ? (
          <GlassCard className="progress-loading-card">
            <SkeletonBlock label="Đang tải tiến độ luyện tập" />
          </GlassCard>
        ) : isAuthenticated && state?.progress ? (
          <div className="progress-layout">
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
                    <strong>{band}</strong>
                  </li>
                ))}
              </ul>
            </GlassCard>
            <div className="progress-side-column">
              <ExamCountdownCard examDate={state.user?.examDate} />
              <CommonMistakesWidget mistakes={state.mistakes} />
            </div>
          </div>
        ) : (
          <GlassCard className="progress-guest-card">
            <div>
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
          </GlassCard>
        )}
      </div>
    </AnimatedSection>
  )
}

export default ProgressOverviewSection
