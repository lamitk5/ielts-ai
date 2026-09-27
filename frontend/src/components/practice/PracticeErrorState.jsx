function PracticeErrorState({ message = 'Chưa thể tải bài luyện tập.', onRetry }) {
  return <div className="practice-error-state" role="alert"><p>{message}</p>{onRetry ? <button type="button" className="button button-secondary button-sm" onClick={onRetry}>Thử lại</button> : null}</div>
}

export default PracticeErrorState
