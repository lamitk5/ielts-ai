import GlassCard from '../common/GlassCard'

const JOB_LABELS = { PENDING: 'Chờ xử lý', PROCESSING: 'Đang xử lý', COMPLETED: 'Hoàn tất', FAILED: 'Thất bại' }

function RagJobList({ jobs }) {
  return <GlassCard className="admin-jobs-card"><p className="eyebrow">XỬ LÝ TÀI LIỆU</p><h2 className="font-display">Tác vụ gần đây</h2><ul className="admin-job-list">{jobs.map((job) => <li key={job.id}><span>{job.id}</span><strong>{JOB_LABELS[job.status] || job.status}</strong></li>)}</ul></GlassCard>
}

export default RagJobList
