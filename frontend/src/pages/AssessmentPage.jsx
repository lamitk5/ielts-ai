import { Link } from 'react-router-dom'
import GlassCard from '../components/common/GlassCard'

const assessmentPaths = [
  { name: 'Reading', route: '/practice/reading', copy: 'Làm quen với dạng câu hỏi và đo độ chính xác theo từng bài.' },
  { name: 'Listening', route: '/practice/listening', copy: 'Luyện nghe theo ngữ cảnh và theo dõi kết quả sau khi nộp bài.' },
  { name: 'Writing', route: '/practice/writing', copy: 'Gửi bài viết để nhận phản hồi có cấu trúc và minh bạch.' },
  { name: 'Speaking', route: '/practice/speaking', copy: 'Luyện câu trả lời văn bản trong khi chờ tích hợp STT.' },
]

function AssessmentPage() {
  return (
    <section className="assessment-page" aria-labelledby="assessment-title">
      <div className="assessment-page-header">
        <p className="eyebrow">LỘ TRÌNH CÁ NHÂN</p>
        <h1 id="assessment-title" className="font-display">Đánh giá năng lực IELTS</h1>
        <p className="foundation-copy">Bắt đầu bằng một kỹ năng bạn muốn hiểu rõ hơn. Kết quả luyện tập sẽ được lưu khi bạn đăng nhập; không có điểm thi chính thức hay band giả.</p>
      </div>
      <div className="assessment-paths">
        {assessmentPaths.map((path) => (
          <GlassCard className="assessment-path" key={path.name}>
            <p className="assessment-path-label">{path.name}</p>
            <p>{path.copy}</p>
            <Link className="button btn-liquid button-secondary button-md" to={path.route}>Bắt đầu với {path.name}</Link>
          </GlassCard>
        ))}
      </div>
    </section>
  )
}

export default AssessmentPage
