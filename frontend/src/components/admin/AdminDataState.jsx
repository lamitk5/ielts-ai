import SkeletonBlock from '../common/SkeletonBlock'

export function AdminDataState({ loading, error, empty, children }) {
  if (loading) return <SkeletonBlock className="admin-data-loading" label="Đang tải dữ liệu quản trị…" />
  if (error) return <p className="admin-error" role="alert">{error}</p>
  if (empty) return <div className="admin-empty"><strong>Chưa có dữ liệu</strong><span>Dữ liệu sẽ xuất hiện khi có hoạt động thực tế.</span></div>
  return children
}
