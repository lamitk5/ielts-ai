import GlassCard from '../common/GlassCard'
import './workspace.css'

export function WritingPromptPane({
  tasks = [],
  selectedTaskId,
  onSelectTask,
  selectedTask,
  wordCount = 0,
}) {
  const currentTask = selectedTask ?? tasks.find((t) => t.id === selectedTaskId) ?? tasks[0]

  return (
    <article
      className="writing-prompt-pane writing-prompt-pane-editorial"
      data-writing-target-id={currentTask?.id}
      tabIndex={-1}
    >
      <div className="writing-prompt-header">
        <p className="writing-pane-kicker">NỘI DUNG ĐỀ BÀI</p>
        <div className="writing-task-select-wrapper">
          <label htmlFor="writing-task" className="writing-task-label">
            Chọn dạng bài
          </label>
          <select
            id="writing-task"
            className="writing-task-select"
            value={currentTask?.id}
            onChange={(event) => onSelectTask?.(event.target.value)}
          >
            {tasks.map((task) => (
              <option key={task.id} value={task.id}>
                {task.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <GlassCard className="writing-prompt-card writing-prompt-card-editorial" role="region" aria-label="Nội dung đề bài chi tiết">
        <h2 className="writing-prompt-title font-display">{currentTask?.prompt}</h2>
        <div className="writing-prompt-meta">
          <span className="writing-meta-pill">
            Tối thiểu {currentTask?.minimumWords ?? 150} từ
          </span>
          <span className="writing-meta-pill">
            Hiện có {wordCount} từ
          </span>
        </div>
        <div className="writing-prompt-instructions">
          <p className="writing-instruction-title">Hướng dẫn làm bài:</p>
          <ul className="writing-instruction-list">
            <li>Đọc kỹ yêu cầu đề bài và xác định các điểm then chốt cần phân tích.</li>
            <li>Sử dụng cấu trúc đoạn mạch lạc (Mở bài, Thân bài, Kết luận).</li>
            <li>Sau khi hoàn thành, nhấn <strong>Gửi bài viết</strong> để nhận phân tích và ước lượng band điểm từ Én.</li>
          </ul>
        </div>
      </GlassCard>
    </article>
  )
}

export default WritingPromptPane
