function PracticeAttemptStatus({ status, error }) {
  if (error) return <p className="auth-error" role="alert">{error}</p>
  if (!status) return null
  return <p className="practice-result-status" role="status">{status === 'FEEDBACK_READY' ? 'Đã nộp và sẵn sàng xem kết quả.' : `Trạng thái: ${status}`}</p>
}

export default PracticeAttemptStatus
