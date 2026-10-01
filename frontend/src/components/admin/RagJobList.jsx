import GlassCard from '../common/GlassCard'

function RagJobList({ jobs }) {
  return <GlassCard className="admin-jobs-card"><p className="eyebrow">INGESTION</p><h2 className="font-display">Job gần đây</h2><ul className="admin-job-list">{jobs.map((job) => <li key={job.id}><span>{job.id}</span><strong>{job.status}</strong></li>)}</ul></GlassCard>
}

export default RagJobList
